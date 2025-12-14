# backend/models.py
from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text
from sqlalchemy.orm import relationship
from database import Base
from datetime import datetime
import enum


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
    
    # Relations CORRIGÉES avec foreign_keys explicites
    documents = relationship("MedicalDocument", back_populates="user", cascade="all, delete-orphan")
    
    # Rendez-vous en tant que patient
    patient_appointments = relationship(
        "Appointment",
        foreign_keys="Appointment.patient_id",
        back_populates="patient",
        cascade="all, delete-orphan"
    )
    
    # Rendez-vous en tant que médecin
    doctor_appointments = relationship(
        "Appointment",
        foreign_keys="Appointment.doctor_id",
        back_populates="doctor",
        cascade="all, delete-orphan"
    )


class MedicalDocument(Base):
    __tablename__ = "medical_documents"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    
    # Informations sur le fichier
    filename = Column(String, nullable=False, unique=True)
    original_filename = Column(String, nullable=False)
    file_type = Column(String, nullable=False)
    file_size = Column(Integer, nullable=False)
    mime_type = Column(String, nullable=True)
    
    # Dates
    upload_date = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relation
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
    
    # Relations CORRIGÉES avec back_populates
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