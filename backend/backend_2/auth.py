# backend/routers/auth.py
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from database import get_db
from models import User
from pydantic import BaseModel
import jwt
from datetime import datetime, timedelta
import os

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
    region: str = None  # Optionnel


class UserResponse(BaseModel):
    id: int
    name: str
    email: str
    role: str = None
    region: str = None

    class Config:
        from_attributes = True

# backend/routers/auth.py

class UpdateProfileRequest(BaseModel):
    name: str
    phone: str = None
    region: str = None
    address: str = None


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
    
    # Mettre à jour les champs
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


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str


@router.put("/change-password/{user_id}")
def change_password(user_id: int, request: ChangePasswordRequest, db: Session = Depends(get_db)):
    """Changer le mot de passe"""
    user = db.query(User).filter(User.id == user_id).first()
    
    if not user:
        return {
            "success": False,
            "message": "Utilisateur non trouvé"
        }
    
    # Vérifier l'ancien mot de passe
    if user.password != request.current_password:
        return {
            "success": False,
            "message": "Mot de passe actuel incorrect"
        }
    
    # Mettre à jour le mot de passe
    user.password = request.new_password
    db.commit()
    
    return {
        "success": True,
        "message": "Mot de passe modifié avec succès"
    }

# ===== FONCTION TOKEN =====
def create_access_token(data: dict, expires_delta: timedelta = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


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
    # Vérifier si l'email existe déjà
    existing_user = db.query(User).filter(User.email == request.email).first()
    
    if existing_user:
        return {
            "success": False,
            "message": "Cet email est déjà utilisé",
            "token": None,
            "user": None
        }

    # Créer le nouvel utilisateur
    new_user = User(
        name=request.name,
        email=request.email,
        password=request.password, 
        region=request.region
    )
    
    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    # Créer un token pour l'utilisateur
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