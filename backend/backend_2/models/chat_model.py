from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List

class MessageCreate(BaseModel):
    conversation_id: int
    content: str
    message_type: str = "text"
    file_url: Optional[str] = None

class MessageResponse(BaseModel):
    id: int
    conversation_id: int
    sender_id: int
    content: str
    message_type: str
    file_url: Optional[str]
    is_read: bool
    created_at: datetime
    
    class Config:
        from_attributes = True

class ConversationResponse(BaseModel):
    id: int
    patient_id: int
    medecin_id: int
    patient_name: str
    medecin_name: str
    last_message: Optional[str]
    last_message_at: Optional[datetime]
    unread_count: int