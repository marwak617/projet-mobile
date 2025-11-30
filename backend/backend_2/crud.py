# backend/crud.py
"""
Opérations CRUD (Create, Read, Update, Delete) pour tous les modèles
"""

from sqlalchemy.orm import Session
from models import User, Doctor, Appointment, MedicalDocument
from datetime import datetime
from typing import List, Optional


# ==================== USERS ====================

def get_user_by_email(db: Session, email: str) -> Optional[User]:
    """Récupérer un utilisateur par email"""
    return db.query(User).filter(User.email == email).first()


def get_user_by_id(db: Session, user_id: int) -> Optional[User]:
    """Récupérer un utilisateur par ID"""
    return db.query(User).filter(User.id == user_id).first()


def create_user(db: Session, name: str, email: str, password: str, 
                region: str = None, role: str = "patient", 
                phone: str = None, address: str = None) -> User:
    """Créer un nouvel utilisateur"""
    user = User(
        name=name, 
        email=email, 
        password=password,  # ⚠️ En production, utilisez un hash (bcrypt)
        region=region,
        role=role,
        phone=phone,
        address=address
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def update_user(db: Session, user_id: int, **kwargs) -> Optional[User]:
    """Mettre à jour un utilisateur"""
    user = get_user_by_id(db, user_id)
    if not user:
        return None
    
    for key, value in kwargs.items():
        if hasattr(user, key) and value is not None:
            setattr(user, key, value)
    
    db.commit()
    db.refresh(user)
    return user


def delete_user(db: Session, user_id: int) -> bool:
    """Supprimer un utilisateur"""
    user = get_user_by_id(db, user_id)
    if not user:
        return False
    
    db.delete(user)
    db.commit()
    return True


def list_users(db: Session, skip: int = 0, limit: int = 100) -> List[User]:
    """Lister tous les utilisateurs"""
    return db.query(User).offset(skip).limit(limit).all()


# ==================== DOCTORS ====================

def get_doctor_by_id(db: Session, doctor_id: int) -> Optional[Doctor]:
    """Récupérer un médecin par ID"""
    return db.query(Doctor).filter(Doctor.id == doctor_id).first()


def create_doctor(db: Session, name: str, speciality: str = None, 
                  city: str = None, latitude: str = None, 
                  longitude: str = None) -> Doctor:
    """Créer un nouveau médecin"""
    doctor = Doctor(
        name=name,
        speciality=speciality,
        city=city,
        latitude=latitude,
        longitude=longitude
    )
    db.add(doctor)
    db.commit()
    db.refresh(doctor)
    return doctor


def update_doctor(db: Session, doctor_id: int, **kwargs) -> Optional[Doctor]:
    """Mettre à jour un médecin"""
    doctor = get_doctor_by_id(db, doctor_id)
    if not doctor:
        return None
    
    for key, value in kwargs.items():
        if hasattr(doctor, key) and value is not None:
            setattr(doctor, key, value)
    
    db.commit()
    db.refresh(doctor)
    return doctor


def delete_doctor(db: Session, doctor_id: int) -> bool:
    """Supprimer un médecin"""
    doctor = get_doctor_by_id(db, doctor_id)
    if not doctor:
        return False
    
    db.delete(doctor)
    db.commit()
    return True


def list_doctors(db: Session, skip: int = 0, limit: int = 100, 
                 city: str = None, speciality: str = None) -> List[Doctor]:
    """Lister les médecins avec filtres optionnels"""
    query = db.query(Doctor)
    
    if city:
        query = query.filter(Doctor.city == city)
    
    if speciality:
        query = query.filter(Doctor.speciality == speciality)
    
    return query.offset(skip).limit(limit).all()


def search_doctors(db: Session, search_term: str) -> List[Doctor]:
    """Rechercher des médecins par nom, spécialité ou ville"""
    return db.query(Doctor).filter(
        (Doctor.name.ilike(f"%{search_term}%")) |
        (Doctor.speciality.ilike(f"%{search_term}%")) |
        (Doctor.city.ilike(f"%{search_term}%"))
    ).all()


# ==================== APPOINTMENTS ====================

def get_appointment_by_id(db: Session, appointment_id: int) -> Optional[Appointment]:
    """Récupérer un rendez-vous par ID"""
    return db.query(Appointment).filter(Appointment.id == appointment_id).first()


def create_appointment(db: Session, user_id: int, doctor_id: int, 
                       date: datetime, status: str = "pending") -> Appointment:
    """Créer un nouveau rendez-vous"""
    appointment = Appointment(
        user_id=user_id,
        doctor_id=doctor_id,
        date=date,
        status=status
    )
    db.add(appointment)
    db.commit()
    db.refresh(appointment)
    return appointment


def update_appointment(db: Session, appointment_id: int, **kwargs) -> Optional[Appointment]:
    """Mettre à jour un rendez-vous"""
    appointment = get_appointment_by_id(db, appointment_id)
    if not appointment:
        return None
    
    for key, value in kwargs.items():
        if hasattr(appointment, key) and value is not None:
            setattr(appointment, key, value)
    
    db.commit()
    db.refresh(appointment)
    return appointment


def delete_appointment(db: Session, appointment_id: int) -> bool:
    """Supprimer un rendez-vous"""
    appointment = get_appointment_by_id(db, appointment_id)
    if not appointment:
        return False
    
    db.delete(appointment)
    db.commit()
    return True


def list_user_appointments(db: Session, user_id: int) -> List[Appointment]:
    """Lister tous les rendez-vous d'un utilisateur"""
    return db.query(Appointment)\
        .filter(Appointment.user_id == user_id)\
        .order_by(Appointment.date.desc())\
        .all()


def list_doctor_appointments(db: Session, doctor_id: int) -> List[Appointment]:
    """Lister tous les rendez-vous d'un médecin"""
    return db.query(Appointment)\
        .filter(Appointment.doctor_id == doctor_id)\
        .order_by(Appointment.date.desc())\
        .all()


def list_appointments_by_status(db: Session, user_id: int, status: str) -> List[Appointment]:
    """Lister les rendez-vous d'un utilisateur par statut"""
    return db.query(Appointment)\
        .filter(Appointment.user_id == user_id, Appointment.status == status)\
        .order_by(Appointment.date.desc())\
        .all()


def get_upcoming_appointments(db: Session, user_id: int) -> List[Appointment]:
    """Récupérer les rendez-vous à venir d'un utilisateur"""
    return db.query(Appointment)\
        .filter(
            Appointment.user_id == user_id,
            Appointment.date >= datetime.now(),
            Appointment.status.in_(["pending", "confirmed"])
        )\
        .order_by(Appointment.date.asc())\
        .all()


# ==================== MEDICAL DOCUMENTS ====================

def get_document_by_id(db: Session, document_id: int) -> Optional[MedicalDocument]:
    """Récupérer un document par ID"""
    return db.query(MedicalDocument).filter(MedicalDocument.id == document_id).first()


def get_document_by_filename(db: Session, filename: str) -> Optional[MedicalDocument]:
    """Récupérer un document par nom de fichier"""
    return db.query(MedicalDocument).filter(MedicalDocument.filename == filename).first()


def create_document(db: Session, user_id: int, filename: str, 
                   original_filename: str, file_type: str, 
                   file_size: int, mime_type: str = None) -> MedicalDocument:
    """Créer un nouveau document médical"""
    document = MedicalDocument(
        user_id=user_id,
        filename=filename,
        original_filename=original_filename,
        file_type=file_type,
        file_size=file_size,
        mime_type=mime_type
    )
    db.add(document)
    db.commit()
    db.refresh(document)
    return document


def update_document(db: Session, document_id: int, **kwargs) -> Optional[MedicalDocument]:
    """Mettre à jour un document"""
    document = get_document_by_id(db, document_id)
    if not document:
        return None
    
    for key, value in kwargs.items():
        if hasattr(document, key) and value is not None:
            setattr(document, key, value)
    
    db.commit()
    db.refresh(document)
    return document


def delete_document(db: Session, document_id: int) -> bool:
    """Supprimer un document"""
    document = get_document_by_id(db, document_id)
    if not document:
        return False
    
    db.delete(document)
    db.commit()
    return True


def list_user_documents(db: Session, user_id: int, 
                       file_type: str = None) -> List[MedicalDocument]:
    """Lister tous les documents d'un utilisateur"""
    query = db.query(MedicalDocument).filter(MedicalDocument.user_id == user_id)
    
    if file_type:
        query = query.filter(MedicalDocument.file_type == file_type)
    
    return query.order_by(MedicalDocument.upload_date.desc()).all()


def count_user_documents(db: Session, user_id: int) -> int:
    """Compter le nombre de documents d'un utilisateur"""
    return db.query(MedicalDocument)\
        .filter(MedicalDocument.user_id == user_id)\
        .count()


def get_documents_by_type(db: Session, user_id: int, file_type: str) -> List[MedicalDocument]:
    """Récupérer les documents d'un utilisateur par type"""
    return db.query(MedicalDocument)\
        .filter(
            MedicalDocument.user_id == user_id,
            MedicalDocument.file_type == file_type
        )\
        .order_by(MedicalDocument.upload_date.desc())\
        .all()


# ==================== STATISTIQUES ====================

def get_user_stats(db: Session, user_id: int) -> dict:
    """Récupérer les statistiques d'un utilisateur"""
    total_appointments = db.query(Appointment)\
        .filter(Appointment.user_id == user_id)\
        .count()
    
    pending_appointments = db.query(Appointment)\
        .filter(Appointment.user_id == user_id, Appointment.status == "pending")\
        .count()
    
    total_documents = db.query(MedicalDocument)\
        .filter(MedicalDocument.user_id == user_id)\
        .count()
    
    return {
        "total_appointments": total_appointments,
        "pending_appointments": pending_appointments,
        "total_documents": total_documents
    }