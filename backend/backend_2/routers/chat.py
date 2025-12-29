from fastapi import APIRouter, WebSocket, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import List, Optional
from pydantic import BaseModel
from datetime import datetime
import os
import uuid

from database import get_db
from models import Conversation, Message, User
from websocket_manager import manager

router = APIRouter(prefix="/chat", tags=["Chat"])

# ============================================
# PYDANTIC SCHEMAS (MODELS DE RÉPONSE)
# ============================================

class MessageResponse(BaseModel):
    id: int
    conversation_id: int
    sender_id: int
    content: str
    message_type: str
    file_url: Optional[str] = None
    is_read: bool
    created_at: datetime
    sender_name: Optional[str] = None
    
    class Config:
        from_attributes = True

class ConversationResponse(BaseModel):
    id: int
    patient_id: int
    medecin_id: int
    patient_name: str
    medecin_name: str
    last_message: Optional[str] = None
    last_message_at: Optional[datetime] = None
    unread_count: int = 0
    
    class Config:
        from_attributes = True

class StatusResponse(BaseModel):
    status: str

# ============================================
# FONCTIONS UTILITAIRES
# ============================================

async def save_message(
    conversation_id: int,
    sender_id: int,
    content: str,
    message_type: str,
    db: Session,
    file_url: Optional[str] = None
) -> Message:
    """Sauvegarder un message dans la base de données"""
    message = Message(
        conversation_id=conversation_id,
        sender_id=sender_id,
        content=content,
        message_type=message_type,
        file_url=file_url,
        is_read=False
    )
    db.add(message)
    db.commit()
    db.refresh(message)
    
    # Mettre à jour last_message_at dans la conversation
    conversation = db.query(Conversation).filter(Conversation.id == conversation_id).first()
    if conversation:
        conversation.last_message_at = message.created_at
        db.commit()
    
    return message

async def get_conversation(conversation_id: int, db: Session) -> Optional[Conversation]:
    """Récupérer une conversation"""
    return db.query(Conversation).filter(Conversation.id == conversation_id).first()

# ============================================
# WEBSOCKET ENDPOINT - VERSION UNIQUE CORRIGÉE
# ============================================

@router.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: int):
    """WebSocket pour la messagerie en temps réel"""
    
    # Obtenir la session DB
    from database import SessionLocal
    db = SessionLocal()
    
    # Récupérer le nom de l'utilisateur AVANT de connecter
    try:
        sender = db.query(User).filter(User.id == user_id).first()
        sender_name = sender.name if sender else f"User {user_id}"
    except Exception as e:
        print(f"❌ Error fetching user {user_id}: {e}")
        sender_name = f"User {user_id}"
    
    # Connecter le websocket
    await manager.connect(websocket, user_id)
    print(f"🔌 WebSocket connected for {sender_name} (ID: {user_id})")
    
    try:
        while True:
            # Recevoir les données
            data = await websocket.receive_json()
            print(f"📩 Received from {sender_name}: {data}")
            
            # Valider les données reçues
            if not all(key in data for key in ["conversation_id", "content"]):
                await websocket.send_json({
                    "type": "error",
                    "message": "Missing required fields: conversation_id, content"
                })
                continue
            
            # Sauvegarder le message en DB
            try:
                message = await save_message(
                    conversation_id=data["conversation_id"],
                    sender_id=user_id,
                    content=data["content"],
                    message_type=data.get("message_type", "text"),
                    db=db
                )
            except Exception as e:
                print(f"❌ Error saving message: {e}")
                await websocket.send_json({
                    "type": "error",
                    "message": f"Failed to save message: {str(e)}"
                })
                continue
            
            # Récupérer la conversation et envoyer aux participants
            conversation = await get_conversation(data["conversation_id"], db)
            
            if conversation:
                # Déterminer le destinataire
                recipient_id = (
                    conversation.medecin_id 
                    if user_id == conversation.patient_id 
                    else conversation.patient_id
                )
                
                # Construire le message à envoyer
                message_data = {
                    "type": "new_message",
                    "message": {
                        "id": message.id,
                        "conversation_id": message.conversation_id,
                        "sender_id": message.sender_id,
                        "sender_name": sender_name,
                        "content": message.content,
                        "message_type": message.message_type,
                        "file_url": message.file_url,
                        "created_at": message.created_at.isoformat(),
                        "is_read": False
                    }
                }
                
                print(f"📤 Broadcasting message to users: [{user_id}, {recipient_id}]")
                
                # Envoyer à tous les participants
                await manager.broadcast_to_conversation(
                    message_data, 
                    [user_id, recipient_id]
                )
                
                print(f"✅ Message broadcasted successfully")
            else:
                print(f"⚠️ Conversation {data['conversation_id']} not found")
                await websocket.send_json({
                    "type": "error",
                    "message": "Conversation not found"
                })
    
    except Exception as e:
        print(f"❌ WebSocket error for user {user_id}: {e}")
        import traceback
        traceback.print_exc()
    
    finally:
        # Toujours déconnecter proprement
        print(f"🔌 Disconnecting user {user_id}")
        await manager.disconnect(websocket, user_id)
        db.close()
        print(f"✅ User {user_id} fully disconnected")

# ============================================
# REST ENDPOINTS
# ============================================

@router.get("/conversations", response_model=List[ConversationResponse])
async def get_user_conversations(
    user_id: int,
    db: Session = Depends(get_db)
):
    """Récupérer toutes les conversations d'un utilisateur"""
    
    conversations = db.query(Conversation).filter(
        (Conversation.patient_id == user_id) | (Conversation.medecin_id == user_id)
    ).all()
    
    result = []
    for conv in conversations:
        # Récupérer les noms
        patient = db.query(User).filter(User.id == conv.patient_id).first()
        medecin = db.query(User).filter(User.id == conv.medecin_id).first()
        
        # Récupérer le dernier message
        last_msg = db.query(Message).filter(
            Message.conversation_id == conv.id
        ).order_by(Message.created_at.desc()).first()
        
        # Compter les messages non lus
        unread_count = db.query(Message).filter(
            Message.conversation_id == conv.id,
            Message.sender_id != user_id,
            Message.is_read == False
        ).count()
        
        result.append(ConversationResponse(
            id=conv.id,
            patient_id=conv.patient_id,
            medecin_id=conv.medecin_id,
            patient_name=patient.name if patient else "Unknown",
            medecin_name=medecin.name if medecin else "Unknown",
            last_message=last_msg.content if last_msg else None,
            last_message_at=conv.last_message_at,
            unread_count=unread_count
        ))
    
    # Trier par date du dernier message
    result.sort(key=lambda x: x.last_message_at or datetime.min, reverse=True)
    
    return result

@router.get("/conversations/{conversation_id}/messages", response_model=List[MessageResponse])
async def get_conversation_messages(
    conversation_id: int,
    user_id: int,
    limit: int = 50,
    offset: int = 0,
    db: Session = Depends(get_db)
):
    """Récupérer les messages d'une conversation"""
    
    # Vérifier que l'utilisateur fait partie de la conversation
    conversation = db.query(Conversation).filter(
        Conversation.id == conversation_id,
        (Conversation.patient_id == user_id) | (Conversation.medecin_id == user_id)
    ).first()
    
    if not conversation:
        raise HTTPException(status_code=403, detail="Accès non autorisé")
    
    # Récupérer les messages (du plus ancien au plus récent pour l'affichage)
    messages = db.query(Message).filter(
        Message.conversation_id == conversation_id
    ).order_by(
        Message.created_at.asc()  # ✅ ASC pour ordre chronologique
    ).limit(limit).offset(offset).all()
    
    # Enrichir avec le nom de l'expéditeur
    result = []
    for msg in messages:
        sender = db.query(User).filter(User.id == msg.sender_id).first()
        result.append(MessageResponse(
            id=msg.id,
            conversation_id=msg.conversation_id,
            sender_id=msg.sender_id,
            content=msg.content,
            message_type=msg.message_type,
            file_url=msg.file_url,
            is_read=msg.is_read,
            created_at=msg.created_at,
            sender_name=sender.name if sender else None
        ))
    
    return result

@router.post("/conversations/{conversation_id}/read", response_model=StatusResponse)
async def mark_messages_as_read(
    conversation_id: int,
    user_id: int,
    db: Session = Depends(get_db)
):
    """Marquer les messages comme lus"""
    
    # Vérifier l'accès
    conversation = db.query(Conversation).filter(
        Conversation.id == conversation_id,
        (Conversation.patient_id == user_id) | (Conversation.medecin_id == user_id)
    ).first()
    
    if not conversation:
        raise HTTPException(status_code=403, detail="Accès non autorisé")
    
    # Marquer comme lus
    db.query(Message).filter(
        Message.conversation_id == conversation_id,
        Message.sender_id != user_id,
        Message.is_read == False
    ).update({"is_read": True})
    
    db.commit()
    
    return StatusResponse(status="success")

@router.post("/upload")
async def upload_file(
    file: UploadFile = File(...),
    conversation_id: int = Form(...),
    sender_id: int = Form(...),
    db: Session = Depends(get_db)
):
    """Upload un fichier (image, document)"""
    
    # Vérifier l'accès à la conversation
    conversation = db.query(Conversation).filter(
        Conversation.id == conversation_id,
        (Conversation.patient_id == sender_id) | (Conversation.medecin_id == sender_id)
    ).first()
    
    if not conversation:
        raise HTTPException(status_code=403, detail="Accès non autorisé")
    
    # Créer le dossier uploads s'il n'existe pas
    UPLOAD_DIR = "uploads/chat"
    os.makedirs(UPLOAD_DIR, exist_ok=True)
    
    # Vérifier la taille du fichier (max 10MB)
    content = await file.read()
    if len(content) > 10 * 1024 * 1024:  # 10MB
        raise HTTPException(status_code=400, detail="Fichier trop volumineux (max 10MB)")
    
    # Générer un nom unique
    file_extension = os.path.splitext(file.filename)[1]
    unique_filename = f"{uuid.uuid4()}{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)
    
    # Sauvegarder le fichier
    with open(file_path, "wb") as buffer:
        buffer.write(content)
    
    # Déterminer le type de message
    mime_type = file.content_type or "application/octet-stream"
    if mime_type.startswith("image/"):
        message_type = "image"
    elif mime_type in ["application/pdf", "application/msword", 
                       "application/vnd.openxmlformats-officedocument.wordprocessingml.document"]:
        message_type = "document"
    else:
        message_type = "document"
    
    # Récupérer le nom de l'expéditeur
    sender = db.query(User).filter(User.id == sender_id).first()
    sender_name = sender.name if sender else None
    
    # Créer un message avec le fichier
    file_url = f"/uploads/chat/{unique_filename}"
    message = Message(
        conversation_id=conversation_id,
        sender_id=sender_id,
        content=file.filename,
        message_type=message_type,
        file_url=file_url,
        is_read=False
    )
    db.add(message)
    db.commit()
    db.refresh(message)
    
    # Mettre à jour la conversation
    conversation.last_message_at = message.created_at
    db.commit()
    
    return {
        "success": True,
        "message": {
            "id": message.id,
            "conversation_id": message.conversation_id,
            "sender_id": message.sender_id,
            "sender_name": sender_name,  # ✅ Ajouté
            "content": message.content,
            "message_type": message.message_type,
            "file_url": message.file_url,
            "created_at": message.created_at.isoformat(),
            "is_read": False
        },
        "file_info": {
            "filename": unique_filename,
            "original_name": file.filename,
            "url": file_url,
            "size": len(content)
        }
    }

@router.get("/download/{filename}")
async def download_file(filename: str):
    """Télécharger un fichier"""
    from fastapi.responses import FileResponse
    
    file_path = os.path.join("uploads/chat", filename)
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Fichier non trouvé")
    
    return FileResponse(
        file_path,
        media_type="application/octet-stream",
        filename=filename
    )

@router.post("/conversations/create")
async def create_or_get_conversation(
    patient_id: int = Form(...),
    medecin_id: int = Form(...),
    db: Session = Depends(get_db)
):
    """Créer une conversation entre patient et médecin (ou retourner si elle existe)"""
    
    # Vérifier si la conversation existe déjà
    existing_conv = db.query(Conversation).filter(
        Conversation.patient_id == patient_id,
        Conversation.medecin_id == medecin_id
    ).first()
    
    if existing_conv:
        return {
            "success": True,
            "conversation_id": existing_conv.id,
            "message": "Conversation existante"
        }
    
    # Créer une nouvelle conversation
    new_conversation = Conversation(
        patient_id=patient_id,
        medecin_id=medecin_id,
        last_message_at=datetime.now()
    )
    db.add(new_conversation)
    db.commit()
    db.refresh(new_conversation)
    
    return {
        "success": True,
        "conversation_id": new_conversation.id,
        "message": "Nouvelle conversation créée"
    }

@router.delete("/messages/{message_id}/attachment")
async def delete_attachment(
    message_id: int,
    user_id: int,
    db: Session = Depends(get_db)
):
    """Supprimer un message avec pièce jointe"""
    
    # Vérifier que l'utilisateur est l'expéditeur
    message = db.query(Message).filter(
        Message.id == message_id,
        Message.sender_id == user_id
    ).first()
    
    if not message:
        raise HTTPException(status_code=403, detail="Accès non autorisé")
    
    # Supprimer le fichier physique si présent
    if message.file_url:
        filename = message.file_url.split("/")[-1]
        file_path = os.path.join("uploads/chat", filename)
        if os.path.exists(file_path):
            os.remove(file_path)
    
    # Supprimer le message
    db.delete(message)
    db.commit()
    
    return {"success": True}