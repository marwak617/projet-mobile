# backend/models.py
from sqlalchemy import Column, Integer, String, ForeignKey, DateTime, Text
from sqlalchemy.orm import relationship
from database import Base
from datetime import datetime


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
    
    # Relations
    appointments = relationship("Appointment", back_populates="user", cascade="all, delete-orphan")
    documents = relationship("MedicalDocument", back_populates="user", cascade="all, delete-orphan")


class MedicalDocument(Base):
    __tablename__ = "medical_documents"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    
    # Informations sur le fichier
    filename = Column(String, nullable=False, unique=True)  # Nom stocké sur le serveur
    original_filename = Column(String, nullable=False)  # Nom original du fichier
    file_type = Column(String, nullable=False)  # mutuelle, ordonnance, analyse, radio, autre
    file_size = Column(Integer, nullable=False)  # Taille en octets
    mime_type = Column(String, nullable=True)  # image/jpeg, application/pdf, etc.
    
    # Dates
    upload_date = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relation
    user = relationship("User", back_populates="documents")


class Doctor(Base):
    __tablename__ = "doctors"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    speciality = Column(String, nullable=True)
    city = Column(String, nullable=True)
    latitude = Column(String, nullable=True)
    longitude = Column(String, nullable=True)
    
    # Relation
    appointments = relationship("Appointment", back_populates="doctor", cascade="all, delete-orphan")


class Appointment(Base):
    __tablename__ = "appointments"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    doctor_id = Column(Integer, ForeignKey("doctors.id", ondelete="CASCADE"), nullable=False)
    date = Column(DateTime, default=datetime.utcnow, nullable=False)
    status = Column(String, default="pending")
    
    # Relations
    user = relationship("User", back_populates="appointments")
    doctor = relationship("Doctor", back_populates="appointments")