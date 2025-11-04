from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime

# --- Users ---
class UserCreate(BaseModel):
    name: str
    email: EmailStr
    password: str
    region: Optional[str]

class UserResponse(BaseModel):
    id: int
    name: str
    email: EmailStr
    region: Optional[str]
    mutuelle_url: Optional[str] = None

    class Config:
        orm_mode = True

# --- Auth ---
class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"

class TokenData(BaseModel):
    email: Optional[str] = None

class LoginRequest(BaseModel):
    email: EmailStr
    password: str

# --- Doctor ---
class DoctorCreate(BaseModel):
    name: str
    speciality: Optional[str]
    city: Optional[str]
    latitude: Optional[str]
    longitude: Optional[str]

class DoctorResponse(DoctorCreate):
    id: int
    class Config:
        orm_mode = True

# --- Appointment ---
class AppointmentCreate(BaseModel):
    user_id: int
    doctor_id: int
    date: datetime

class AppointmentResponse(AppointmentCreate):
    id: int
    status: str
    class Config:
        orm_mode = True

# --- Document ---
class DocumentResponse(BaseModel):
    id: int
    filename: str
    file_url: str
    type: Optional[str]
    class Config:
        orm_mode = True
