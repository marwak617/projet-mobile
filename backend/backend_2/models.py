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
    mutuelle_url = Column(String, nullable=True)
    role  = Column(String, nullable=True)
    appointments = relationship("Appointment", back_populates="user")
    documents = relationship("Document", back_populates="user")

class Doctor(Base):
    __tablename__ = "doctors"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    speciality = Column(String, nullable=True)
    city = Column(String, nullable=True)
    latitude = Column(String, nullable=True)
    longitude = Column(String, nullable=True)

    appointments = relationship("Appointment", back_populates="doctor")

class Appointment(Base):
    __tablename__ = "appointments"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    doctor_id = Column(Integer, ForeignKey("doctors.id"))
    date = Column(DateTime, default=datetime.utcnow)
    status = Column(String, default="pending")

    user = relationship("User", back_populates="appointments")
    doctor = relationship("Doctor", back_populates="appointments")

class Document(Base):
    __tablename__ = "documents"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    filename = Column(String, nullable=False)
    file_url = Column(String, nullable=False)
    type = Column(String, nullable=True)

    user = relationship("User", back_populates="documents")
