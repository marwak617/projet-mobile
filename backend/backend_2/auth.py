# backend/routers/auth.py
from fastapi import APIRouter, HTTPException, Depends, File, UploadFile, Form
from sqlalchemy.orm import Session
from database import get_db
from models import User, MedicalDocument
from pydantic import BaseModel
import jwt
from datetime import datetime, timedelta
import os
from typing import List, Optional
import shutil

# Dossier pour stocker les documents
UPLOAD_DIR = "uploads/medical_documents"
os.makedirs(UPLOAD_DIR, exist_ok=True)

router = APIRouter(prefix="/users", tags=["Authentication"])

SECRET_KEY = os.getenv("SECRET_KEY", "mysecret")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60


# ===== MODELS =====
class LoginRequest(BaseModel):
    email: str
    password: str


class RegisterRequest(BaseModel):
    name: str
    email: str
    password: str
    region: str = None


class UserResponse(BaseModel):
    id: int
    name: str
    email: str
    role: str = None
    region: str = None

    class Config:
        from_attributes = True


class UpdateProfileRequest(BaseModel):
    name: str
    phone: str = None
    region: str = None
    address: str = None


class DocumentResponse(BaseModel):
    id: int
    user_id: int
    filename: str
    original_filename: str
    file_type: str
    upload_date: str
    file_size: int
    mime_type: Optional[str] = None

    class Config:
        from_attributes = True


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str


# ===== FONCTION TOKEN =====
def create_access_token(data: dict, expires_delta: timedelta = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


# ===== ENDPOINTS PROFIL =====
@router.get("/profile/{user_id}")
def get_profile(user_id: int, db: Session = Depends(get_db)):
    """Récupérer le profil d'un utilisateur"""
    user = db.query(User).filter(User.id == user_id).first()
    
    if not user:
        return {
            "success": False,
            "message": "Utilisateur non trouvé"
        }
    
    return {
        "success": True,
        "user": {
            "id": user.id,
            "name": user.name,
            "email": user.email,
            "phone": getattr(user, 'phone', None),
            "region": getattr(user, 'region', None),
            "address": getattr(user, 'address', None),
            "role": getattr(user, 'role', 'patient')
        }
    }


@router.put("/profile/{user_id}")
def update_profile(user_id: int, request: UpdateProfileRequest, db: Session = Depends(get_db)):
    """Mettre à jour le profil d'un utilisateur"""
    user = db.query(User).filter(User.id == user_id).first()
    
    if not user:
        return {
            "success": False,
            "message": "Utilisateur non trouvé"
        }
    
    user.name = request.name
    if hasattr(user, 'phone'):
        user.phone = request.phone
    if hasattr(user, 'region'):
        user.region = request.region
    if hasattr(user, 'address'):
        user.address = request.address
    
    db.commit()
    db.refresh(user)
    
    return {
        "success": True,
        "message": "Profil mis à jour avec succès",
        "user": {
            "id": user.id,
            "name": user.name,
            "email": user.email,
            "phone": getattr(user, 'phone', None),
            "region": getattr(user, 'region', None),
            "address": getattr(user, 'address', None),
            "role": getattr(user, 'role', 'patient')
        }
    }


@router.put("/change-password/{user_id}")
def change_password(user_id: int, request: ChangePasswordRequest, db: Session = Depends(get_db)):
    """Changer le mot de passe"""
    user = db.query(User).filter(User.id == user_id).first()
    
    if not user:
        return {
            "success": False,
            "message": "Utilisateur non trouvé"
        }
    
    if user.password != request.current_password:
        return {
            "success": False,
            "message": "Mot de passe actuel incorrect"
        }
    
    user.password = request.new_password
    db.commit()
    
    return {
        "success": True,
        "message": "Mot de passe modifié avec succès"
    }


# ===== ENDPOINT LOGIN =====
@router.post("/login")
def login(request: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == request.email).first()
    
    if not user:
        return {
            "success": False,
            "message": "Email incorrect",
            "token": None,
            "user": None
        }

    if request.password != user.password:
        return {
            "success": False,
            "message": "Mot de passe incorrect",
            "token": None,
            "user": None
        }

    token = create_access_token({"sub": user.email})
    
    return {
        "success": True,
        "message": "Connexion réussie",
        "token": token,
        "user": {
            "id": user.id,
            "name": user.name,
            "email": user.email,
            "role": getattr(user, 'role', None),
            "region": getattr(user, 'region', None)
        }
    }


# ===== ENDPOINT REGISTER =====
@router.post("/register")
def register(request: RegisterRequest, db: Session = Depends(get_db)):
    existing_user = db.query(User).filter(User.email == request.email).first()
    
    if existing_user:
        return {
            "success": False,
            "message": "Cet email est déjà utilisé",
            "token": None,
            "user": None
        }

    new_user = User(
        name=request.name,
        email=request.email,
        password=request.password, 
        region=request.region
    )
    
    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    token = create_access_token({"sub": new_user.email})
    
    return {
        "success": True,
        "message": "Compte créé avec succès",
        "token": token,
        "user": {
            "id": new_user.id,
            "name": new_user.name,
            "email": new_user.email,
            "role": new_user.role,
            "region": new_user.region
        }
    }


# ===== ENDPOINTS DOCUMENTS =====
@router.post("/upload-document/{user_id}")
async def upload_document(
    user_id: int,
    file: UploadFile = File(...),
    document_type: str = Form("mutuelle"),
    db: Session = Depends(get_db)
):
    """Télécharger un document médical"""
    
    # Vérifier que l'utilisateur existe
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        return {
            "success": False,
            "message": "Utilisateur non trouvé"
        }
    
    # Validation du type de document
    valid_types = ["mutuelle", "ordonnance", "analyse", "radio", "autre"]
    if document_type not in valid_types:
        document_type = "autre"
    
    # Générer un nom de fichier unique
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    file_extension = os.path.splitext(file.filename)[1]
    unique_filename = f"{user_id}_{document_type}_{timestamp}{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)
    
    # Sauvegarder le fichier physique
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        file_size = os.path.getsize(file_path)
        
        # Créer l'entrée en base de données
        new_document = MedicalDocument(
            user_id=user_id,
            filename=unique_filename,
            original_filename=file.filename,
            file_type=document_type,
            file_size=file_size,
            mime_type=file.content_type
        )
        
        db.add(new_document)
        db.commit()
        db.refresh(new_document)
        
        return {
            "success": True,
            "message": "Document téléchargé avec succès",
            "document": {
                "id": new_document.id,
                "filename": new_document.filename,
                "original_filename": new_document.original_filename,
                "file_type": new_document.file_type,
                "upload_date": new_document.upload_date.isoformat(),
                "file_size": new_document.file_size,
                "mime_type": new_document.mime_type
            }
        }
        
    except Exception as e:
        # Nettoyer en cas d'erreur
        if os.path.exists(file_path):
            os.remove(file_path)
        db.rollback()
        
        return {
            "success": False,
            "message": f"Erreur lors du téléchargement: {str(e)}"
        }


@router.get("/documents/{user_id}")
def get_user_documents(user_id: int, db: Session = Depends(get_db)):
    """Récupérer la liste des documents d'un utilisateur"""
    
    # Vérifier que l'utilisateur existe
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        return {
            "success": False,
            "message": "Utilisateur non trouvé",
            "documents": []
        }
    
    # Récupérer les documents depuis la base de données
    documents = db.query(MedicalDocument)\
        .filter(MedicalDocument.user_id == user_id)\
        .order_by(MedicalDocument.upload_date.desc())\
        .all()
    
    # Vérifier que les fichiers existent physiquement
    document_list = []
    for doc in documents:
        file_path = os.path.join(UPLOAD_DIR, doc.filename)
        
        # Si le fichier n'existe plus physiquement, on peut le signaler
        if not os.path.exists(file_path):
            print(f"Warning: Fichier {doc.filename} manquant pour le document ID {doc.id}")
        
        document_list.append({
            "id": doc.id,
            "filename": doc.filename,
            "original_filename": doc.original_filename,
            "file_type": doc.file_type,
            "file_size": doc.file_size,
            "upload_date": doc.upload_date.isoformat(),
            "mime_type": doc.mime_type
        })
    
    return {
        "success": True,
        "count": len(document_list),
        "documents": document_list
    }


@router.delete("/document/{user_id}/{filename}")
def delete_document(user_id: int, filename: str, db: Session = Depends(get_db)):
    """Supprimer un document"""
    
    # Vérifier que le document appartient à l'utilisateur
    document = db.query(MedicalDocument)\
        .filter(
            MedicalDocument.user_id == user_id,
            MedicalDocument.filename == filename
        )\
        .first()
    
    if not document:
        return {
            "success": False,
            "message": "Document non trouvé ou accès non autorisé"
        }
    
    file_path = os.path.join(UPLOAD_DIR, filename)
    
    try:
        # Supprimer le fichier physique s'il existe
        if os.path.exists(file_path):
            os.remove(file_path)
        
        # Supprimer l'entrée en base de données
        db.delete(document)
        db.commit()
        
        return {
            "success": True,
            "message": "Document supprimé avec succès"
        }
        
    except Exception as e:
        db.rollback()
        return {
            "success": False,
            "message": f"Erreur lors de la suppression: {str(e)}"
        }


@router.get("/document/download/{user_id}/{filename}")
def download_document(user_id: int, filename: str, db: Session = Depends(get_db)):
    """Télécharger un document"""
    from fastapi.responses import FileResponse
    
    # Vérifier que le document appartient à l'utilisateur
    document = db.query(MedicalDocument)\
        .filter(
            MedicalDocument.user_id == user_id,
            MedicalDocument.filename == filename
        )\
        .first()
    
    if not document:
        raise HTTPException(status_code=404, detail="Document non trouvé")
    
    file_path = os.path.join(UPLOAD_DIR, filename)
    
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Fichier physique non trouvé")
    
    return FileResponse(
        path=file_path,
        filename=document.original_filename,
        media_type=document.mime_type or "application/octet-stream"
    )


