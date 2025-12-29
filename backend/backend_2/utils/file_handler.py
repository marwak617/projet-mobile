import os
import uuid
import aiofiles
from fastapi import UploadFile, HTTPException
from typing import Tuple
import mimetypes

# Configuration
UPLOAD_DIR = "uploads/chat"
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB
ALLOWED_IMAGE_TYPES = {'image/jpeg', 'image/png', 'image/gif', 'image/webp'}
ALLOWED_DOCUMENT_TYPES = {
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain'
}

os.makedirs(UPLOAD_DIR, exist_ok=True)

def get_file_category(mime_type: str) -> str:
    """Détermine la catégorie du fichier"""
    if mime_type in ALLOWED_IMAGE_TYPES:
        return "image"
    elif mime_type in ALLOWED_DOCUMENT_TYPES:
        return "document"
    else:
        return "other"

def validate_file(file: UploadFile, content: bytes) -> Tuple[bool, str]:
    """Valide le fichier uploadé"""
    
    # Vérifier la taille
    if len(content) > MAX_FILE_SIZE:
        return False, "Le fichier est trop volumineux (max 10MB)"
    
    # Vérifier le type MIME
    mime_type = file.content_type
    if mime_type not in ALLOWED_IMAGE_TYPES and mime_type not in ALLOWED_DOCUMENT_TYPES:
        return False, f"Type de fichier non autorisé: {mime_type}"
    
    return True, "OK"

async def save_upload_file(file: UploadFile) -> dict:
    """Sauvegarde le fichier et retourne les informations"""
    
    # Lire le contenu
    content = await file.read()
    
    # Valider
    is_valid, message = validate_file(file, content)
    if not is_valid:
        raise HTTPException(status_code=400, detail=message)
    
    # Générer un nom unique
    file_extension = os.path.splitext(file.filename)[1]
    unique_filename = f"{uuid.uuid4()}{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)
    
    # Sauvegarder
    async with aiofiles.open(file_path, 'wb') as f:
        await f.write(content)
    
    # Déterminer la catégorie
    category = get_file_category(file.content_type)
    
    return {
        "filename": unique_filename,
        "original_name": file.filename,
        "file_path": file_path,
        "url": f"/uploads/chat/{unique_filename}",
        "size": len(content),
        "mime_type": file.content_type,
        "category": category
    }

def generate_thumbnail(image_path: str, thumbnail_path: str, size=(200, 200)):
    """Génère une miniature pour les images"""
    try:
        from PIL import Image
        
        with Image.open(image_path) as img:
            img.thumbnail(size)
            img.save(thumbnail_path, optimize=True, quality=85)
        return True
    except Exception as e:
        print(f"Erreur génération miniature: {e}")
        return False