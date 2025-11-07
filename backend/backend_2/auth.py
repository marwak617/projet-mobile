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