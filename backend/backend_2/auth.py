# backend/routers/auth.py
from fastapi import APIRouter, HTTPException, Depends, status
from sqlalchemy.orm import Session
from database import get_db
from models import User
import crud, schemas
from pydantic import BaseModel
import jwt
from datetime import datetime, timedelta
import os

router = APIRouter(prefix="/users", tags=["Authentication"])

SECRET_KEY = os.getenv("SECRET_KEY", "mysecret")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60


class LoginRequest(BaseModel):
    email: str
    password: str


def create_access_token(data: dict, expires_delta: timedelta = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES))
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)


# ✅ ENDPOINT LOGIN (compatible Android)
@router.post("/login")
def login(request: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == request.email).first()
    
    if not user or request.password != user.password:
        return {
            "success": False,
            "message": "Identifiants incorrects",
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
            "role": getattr(user, 'role', None)
        }
    }


# ✅ ENDPOINT REGISTER
@router.post("/register", response_model=schemas.UserResponse)
def register(user_in: schemas.UserCreate, db: Session = Depends(get_db)):
    existing = crud.get_user_by_email(db, user_in.email)
    if existing:
        raise HTTPException(status_code=400, detail="Email déjà utilisé")
    user = crud.create_user(db, user_in.name, user_in.email, user_in.password, user_in.region)
    return user


# ✅ ENDPOINT GET USER
@router.get("/{user_id}", response_model=schemas.UserResponse)
def get_user(user_id: int, db: Session = Depends(get_db)):
    user = crud.get_user(db, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="Utilisateur non trouvé")
    return user