from fastapi import APIRouter, UploadFile, File, Depends, HTTPException, Form
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from typing import Optional
import os
from utils.file_handler import save_upload_file, UPLOAD_DIR, generate_thumbnail
from database import get_db

router = APIRouter(prefix="/chat", tags=["Chat Files"])

@router.post("/upload")
async def upload_chat_file(
    file: UploadFile = File(...),
    conversation_id: int = Form(...),
    sender_id: int = Form(...),
    db: Session = Depends(get_db)
):
    """Upload un fichier et créer un message associé"""
    
    try:
        # Vérifier que l'utilisateur fait partie de la conversation
        conversation = db.execute(
            """SELECT * FROM conversations 
               WHERE id = :id AND (patient_id = :user_id OR medecin_id = :user_id)""",
            {"id": conversation_id, "user_id": sender_id}
        ).fetchone()
        
        if not conversation:
            raise HTTPException(status_code=403, detail="Accès non autorisé")
        
        # Sauvegarder le fichier
        file_info = await save_upload_file(file)
        
        # Générer miniature pour les images
        thumbnail_url = None
        if file_info["category"] == "image":
            thumbnail_filename = f"thumb_{file_info['filename']}"
            thumbnail_path = os.path.join(UPLOAD_DIR, thumbnail_filename)
            if generate_thumbnail(file_info["file_path"], thumbnail_path):
                thumbnail_url = f"/uploads/chat/{thumbnail_filename}"
        
        # Créer le message en base de données
        message_query = """
            INSERT INTO messages (conversation_id, sender_id, content, message_type, file_url)
            VALUES (:conv_id, :sender_id, :content, :msg_type, :file_url)
            RETURNING id, conversation_id, sender_id, content, message_type, file_url, 
                      is_read, created_at
        """
        
        message = db.execute(
            message_query,
            {
                "conv_id": conversation_id,
                "sender_id": sender_id,
                "content": file_info["original_name"],
                "msg_type": file_info["category"],
                "file_url": file_info["url"]
            }
        ).fetchone()
        
        # Créer l'entrée dans chat_attachments
        attachment_query = """
            INSERT INTO chat_attachments 
            (message_id, file_name, file_type, file_size, file_path)
            VALUES (:msg_id, :name, :type, :size, :path)
        """
        
        db.execute(
            attachment_query,
            {
                "msg_id": message.id,
                "name": file_info["original_name"],
                "type": file_info["mime_type"],
                "size": file_info["size"],
                "path": file_info["file_path"]
            }
        )
        
        # Mettre à jour last_message_at de la conversation
        db.execute(
            "UPDATE conversations SET last_message_at = NOW() WHERE id = :id",
            {"id": conversation_id}
        )
        
        db.commit()
        
        return {
            "success": True,
            "message": {
                "id": message.id,
                "conversation_id": message.conversation_id,
                "sender_id": message.sender_id,
                "content": message.content,
                "message_type": message.message_type,
                "file_url": message.file_url,
                "thumbnail_url": thumbnail_url,
                "created_at": message.created_at.isoformat(),
                "is_read": False
            },
            "file_info": {
                "size": file_info["size"],
                "mime_type": file_info["mime_type"]
            }
        }
        
    except HTTPException:
        raise
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=f"Erreur upload: {str(e)}")

@router.get("/download/{filename}")
async def download_file(filename: str):
    """Télécharger un fichier"""
    file_path = os.path.join(UPLOAD_DIR, filename)
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Fichier non trouvé")
    
    return FileResponse(
        file_path,
        media_type="application/octet-stream",
        filename=filename
    )

@router.get("/preview/{filename}")
async def preview_file(filename: str):
    """Prévisualiser un fichier (image ou miniature)"""
    file_path = os.path.join(UPLOAD_DIR, filename)
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Fichier non trouvé")
    
    # Détecter le type MIME
    mime_type, _ = mimetypes.guess_type(file_path)
    
    return FileResponse(file_path, media_type=mime_type)

@router.delete("/messages/{message_id}/attachment")
async def delete_attachment(
    message_id: int,
    user_id: int,
    db: Session = Depends(get_db)
):
    """Supprimer un fichier attaché à un message"""
    
    # Vérifier que l'utilisateur est l'expéditeur
    message = db.execute(
        "SELECT * FROM messages WHERE id = :id AND sender_id = :user_id",
        {"id": message_id, "user_id": user_id}
    ).fetchone()
    
    if not message:
        raise HTTPException(status_code=403, detail="Accès non autorisé")
    
    # Récupérer les infos du fichier
    attachment = db.execute(
        "SELECT * FROM chat_attachments WHERE message_id = :id",
        {"id": message_id}
    ).fetchone()
    
    if attachment:
        # Supprimer le fichier physique
        if os.path.exists(attachment.file_path):
            os.remove(attachment.file_path)
        
        # Supprimer la miniature si elle existe
        filename = os.path.basename(attachment.file_path)
        thumbnail_path = os.path.join(UPLOAD_DIR, f"thumb_{filename}")
        if os.path.exists(thumbnail_path):
            os.remove(thumbnail_path)
    
    # Supprimer le message
    db.execute("DELETE FROM messages WHERE id = :id", {"id": message_id})
    db.commit()
    
    return {"success": True}