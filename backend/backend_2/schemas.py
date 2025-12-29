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

class MessageBase(BaseModel):
    content: str
    message_type: str = "text"
    file_url: Optional[str] = None

class MessageCreate(MessageBase):
    conversation_id: int

class MessageResponse(MessageBase):
    id: int
    conversation_id: int
    sender_id: int
    is_read: bool
    created_at: datetime
    sender_name: Optional[str] = None
    
    class Config:
        from_attributes = True

class ConversationBase(BaseModel):
    patient_id: int
    medecin_id: int

class ConversationCreate(ConversationBase):
    pass

class ConversationResponse(ConversationBase):
    id: int
    created_at: datetime
    last_message_at: Optional[datetime]
    patient_name: str
    medecin_name: str
    last_message: Optional[str]
    last_message_type: Optional[str]
    unread_count: int = 0
    
    class Config:
        from_attributes = True

class ChatAttachmentResponse(BaseModel):
    id: int
    message_id: int
    file_name: str
    file_type: str
    file_size: Optional[int]
    file_path: str
    thumbnail_path: Optional[str]
    created_at: datetime
    
    class Config:
        from_attributes = True