# backend/models.py
from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text, Boolean, BigInteger
from sqlalchemy.orm import relationship
from database import Base
from datetime import datetime
import enum
import sqlalchemy


class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    password = Column(String, nullable=False)
    region = Column(String, nullable=True)
    role = Column(String, nullable=True, default="patient")
    specialty = Column(String, nullable=True)
    phone = Column(String, nullable=True)
    address = Column(String, nullable=True)
    
    # Relations existantes
    documents = relationship("MedicalDocument", back_populates="user", cascade="all, delete-orphan")
    
    patient_appointments = relationship(
        "Appointment",
        foreign_keys="Appointment.patient_id",
        back_populates="patient",
        cascade="all, delete-orphan"
    )
    
    doctor_appointments = relationship(
        "Appointment",
        foreign_keys="Appointment.doctor_id",
        back_populates="doctor",
        cascade="all, delete-orphan"
    )
    
    # ========== NOUVELLES RELATIONS POUR LE CHAT ==========
    
    # Conversations en tant que patient
    patient_conversations = relationship(
        "Conversation",
        foreign_keys="Conversation.patient_id",
        back_populates="patient",
        cascade="all, delete-orphan"
    )
    
    # Conversations en tant que médecin
    doctor_conversations = relationship(
        "Conversation",
        foreign_keys="Conversation.medecin_id",
        back_populates="medecin",
        cascade="all, delete-orphan"
    )
    
    # Messages envoyés
    sent_messages = relationship(
        "Message",
        foreign_keys="Message.sender_id",
        back_populates="sender",
        cascade="all, delete-orphan"
    )


class MedicalDocument(Base):
    __tablename__ = "medical_documents"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    
    filename = Column(String, nullable=False, unique=True)
    original_filename = Column(String, nullable=False)
    file_type = Column(String, nullable=False)
    file_size = Column(Integer, nullable=False)
    mime_type = Column(String, nullable=True)
    
    upload_date = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    user = relationship("User", back_populates="documents")


class AppointmentStatus(enum.Enum):
    PENDING = "pending"
    CONFIRMED = "confirmed"
    REJECTED = "rejected"
    CANCELLED = "cancelled"
    COMPLETED = "completed"


class Appointment(Base):
    __tablename__ = "appointments"
    
    id = Column(Integer, primary_key=True, index=True)
    patient_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    doctor_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    appointment_date = Column(DateTime, nullable=False)
    status = Column(String, default="pending")
    reason = Column(String, nullable=True)
    notes = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    patient = relationship(
        "User",
        foreign_keys=[patient_id],
        back_populates="patient_appointments"
    )
    
    doctor = relationship(
        "User",
        foreign_keys=[doctor_id],
        back_populates="doctor_appointments"
    )


# ============================================
# NOUVEAUX MODÈLES POUR LE CHAT
# ============================================

class MessageType(enum.Enum):
    TEXT = "text"
    IMAGE = "image"
    DOCUMENT = "document"
    AUDIO = "audio"
    VIDEO = "video"


class Conversation(Base):
    __tablename__ = "conversations"
    
    id = Column(Integer, primary_key=True, index=True)
    patient_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    medecin_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    last_message_at = Column(DateTime, nullable=True)
    
    # Relations
    patient = relationship(
        "User",
        foreign_keys=[patient_id],
        back_populates="patient_conversations"
    )
    
    medecin = relationship(
        "User",
        foreign_keys=[medecin_id],
        back_populates="doctor_conversations"
    )
    
    messages = relationship(
        "Message",
        back_populates="conversation",
        cascade="all, delete-orphan",
        order_by="Message.created_at"
    )
    
    # Contrainte unique pour éviter les doublons
    __table_args__ = (
        sqlalchemy.UniqueConstraint('patient_id', 'medecin_id', name='unique_conversation'),
    )


class Message(Base):
    __tablename__ = "messages"
    
    id = Column(Integer, primary_key=True, index=True)
    conversation_id = Column(Integer, ForeignKey("conversations.id", ondelete="CASCADE"), nullable=False)
    sender_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    content = Column(Text, nullable=False)
    message_type = Column(String, default="text", nullable=False)  # text, image, document, audio, video
    file_url = Column(String(500), nullable=True)
    is_read = Column(Boolean, default=False, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)
    
    # Relations
    conversation = relationship("Conversation", back_populates="messages")
    sender = relationship("User", foreign_keys=[sender_id], back_populates="sent_messages")
    attachments = relationship("ChatAttachment", back_populates="message", cascade="all, delete-orphan")


class ChatAttachment(Base):
    __tablename__ = "chat_attachments"
    
    id = Column(Integer, primary_key=True, index=True)
    message_id = Column(Integer, ForeignKey("messages.id", ondelete="CASCADE"), nullable=False)
    file_name = Column(String(255), nullable=False)
    file_type = Column(String(50), nullable=False)
    file_size = Column(BigInteger, nullable=True)
    file_path = Column(String(500), nullable=False)
    thumbnail_path = Column(String(500), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    
    # Relation
    message = relationship("Message", back_populates="attachments")


class MessageReadStatus(Base):
    """Table optionnelle pour tracker le statut de lecture détaillé"""
    __tablename__ = "message_read_status"
    
    id = Column(Integer, primary_key=True, index=True)
    message_id = Column(Integer, ForeignKey("messages.id", ondelete="CASCADE"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    read_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    
    # Contrainte unique
    __table_args__ = (
        sqlalchemy.UniqueConstraint('message_id', 'user_id', name='unique_message_read'),
    )


class ChatNotification(Base):
    """Table pour les notifications de chat"""
    __tablename__ = "chat_notifications"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    message_id = Column(Integer, ForeignKey("messages.id", ondelete="CASCADE"), nullable=True)
    notification_type = Column(String(50), nullable=False)  # new_message, new_conversation, etc.
    is_read = Column(Boolean, default=False, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)