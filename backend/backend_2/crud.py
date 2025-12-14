# backend/crud.py
"""
Opérations CRUD (Create, Read, Update, Delete) pour tous les modèles
"""

from sqlalchemy.orm import Session
from models import User, Appointment, MedicalDocument
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
                specialty: str = None, phone: str = None, 
                address: str = None) -> User:
    """Créer un nouvel utilisateur"""
    user = User(
        name=name, 
        email=email, 
        password=password,  # ⚠️ En production, utilisez un hash (bcrypt)
        region=region,
        role=role,
        specialty=specialty,
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


def list_users(db: Session, skip: int = 0, limit: int = 100, role: str = None) -> List[User]:
    """Lister tous les utilisateurs"""
    query = db.query(User)
    
    if role:
        query = query.filter(User.role == role)
    
    return query.offset(skip).limit(limit).all()


def list_doctors(db: Session, skip: int = 0, limit: int = 100, 
                 region: str = None, specialty: str = None) -> List[User]:
    """Lister les médecins (users avec role='doctor')"""
    query = db.query(User).filter(User.role == "doctor")
    
    if region:
        query = query.filter(User.region == region)
    
    if specialty:
        query = query.filter(User.specialty == specialty)
    
    return query.offset(skip).limit(limit).all()


def search_doctors(db: Session, search_term: str) -> List[User]:
    """Rechercher des médecins par nom, spécialité ou région"""
    return db.query(User).filter(
        User.role == "doctor",
        (User.name.ilike(f"%{search_term}%")) |
        (User.specialty.ilike(f"%{search_term}%")) |
        (User.region.ilike(f"%{search_term}%"))
    ).all()


# ==================== APPOINTMENTS ====================

def get_appointment_by_id(db: Session, appointment_id: int) -> Optional[Appointment]:
    """Récupérer un rendez-vous par ID"""
    return db.query(Appointment).filter(Appointment.id == appointment_id).first()


def create_appointment(db: Session, patient_id: int, doctor_id: int, 
                       appointment_date: datetime, reason: str = None,
                       notes: str = None, status: str = "pending") -> Appointment:
    """Créer un nouveau rendez-vous"""
    appointment = Appointment(
        patient_id=patient_id,
        doctor_id=doctor_id,
        appointment_date=appointment_date,
        reason=reason,
        notes=notes,
        status=status,
        created_at=datetime.now(),
        updated_at=datetime.now()
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
    
    appointment.updated_at = datetime.now()
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


def list_patient_appointments(db: Session, patient_id: int, 
                              status: str = None) -> List[Appointment]:
    """Lister tous les rendez-vous d'un patient"""
    query = db.query(Appointment).filter(Appointment.patient_id == patient_id)
    
    if status:
        query = query.filter(Appointment.status == status)
    
    return query.order_by(Appointment.appointment_date.desc()).all()


def list_doctor_appointments(db: Session, doctor_id: int, 
                             status: str = None) -> List[Appointment]:
    """Lister tous les rendez-vous d'un médecin"""
    query = db.query(Appointment).filter(Appointment.doctor_id == doctor_id)
    
    if status:
        query = query.filter(Appointment.status == status)
    
    return query.order_by(Appointment.appointment_date.desc()).all()


def get_upcoming_appointments(db: Session, user_id: int, 
                             is_doctor: bool = False) -> List[Appointment]:
    """Récupérer les rendez-vous à venir"""
    if is_doctor:
        query = db.query(Appointment).filter(Appointment.doctor_id == user_id)
    else:
        query = db.query(Appointment).filter(Appointment.patient_id == user_id)
    
    return query.filter(
        Appointment.appointment_date >= datetime.now(),
        Appointment.status.in_(["pending", "confirmed"])
    ).order_by(Appointment.appointment_date.asc()).all()


def get_doctor_availability(db: Session, doctor_id: int, date: datetime) -> List[str]:
    """Récupérer les créneaux occupés pour un médecin à une date donnée"""
    appointments = db.query(Appointment).filter(
        Appointment.doctor_id == doctor_id,
        Appointment.appointment_date >= date,
        Appointment.appointment_date < datetime(date.year, date.month, date.day, 23, 59),
        Appointment.status.in_(["pending", "confirmed"])
    ).all()
    
    return [apt.appointment_date.strftime("%H:%M") for apt in appointments]


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
    
    document.updated_at = datetime.now()
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

def get_user_stats(db: Session, user_id: int, is_doctor: bool = False) -> dict:
    """Récupérer les statistiques d'un utilisateur"""
    if is_doctor:
        total_appointments = db.query(Appointment)\
            .filter(Appointment.doctor_id == user_id)\
            .count()
        
        pending_appointments = db.query(Appointment)\
            .filter(Appointment.doctor_id == user_id, Appointment.status == "pending")\
            .count()
    else:
        total_appointments = db.query(Appointment)\
            .filter(Appointment.patient_id == user_id)\
            .count()
        
        pending_appointments = db.query(Appointment)\
            .filter(Appointment.patient_id == user_id, Appointment.status == "pending")\
            .count()
    
    total_documents = db.query(MedicalDocument)\
        .filter(MedicalDocument.user_id == user_id)\
        .count()
    
    return {
        "total_appointments": total_appointments,
        "pending_appointments": pending_appointments,
        "total_documents": total_documents
    }


def get_doctor_stats(db: Session, doctor_id: int) -> dict:
    """Récupérer les statistiques spécifiques d'un médecin"""
    total_patients = db.query(Appointment.patient_id)\
        .filter(Appointment.doctor_id == doctor_id)\
        .distinct()\
        .count()
    
    confirmed_today = db.query(Appointment)\
        .filter(
            Appointment.doctor_id == doctor_id,
            Appointment.status == "confirmed",
            Appointment.appointment_date >= datetime.now().replace(hour=0, minute=0, second=0),
            Appointment.appointment_date < datetime.now().replace(hour=23, minute=59, second=59)
        )\
        .count()
    
    return {
        **get_user_stats(db, doctor_id, is_doctor=True),
        "total_patients": total_patients,
        "confirmed_today": confirmed_today
    }