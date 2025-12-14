# backend/routers/appointments.py

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from models import Appointment, User
from pydantic import BaseModel
from datetime import datetime
from typing import List, Optional

router = APIRouter(prefix="/appointments", tags=["Appointments"])


# ===== MODELS PYDANTIC =====

class AppointmentCreate(BaseModel):
    doctor_id: int
    appointment_date: str  # Format: "2025-11-15T14:30:00"
    reason: str
    notes: Optional[str] = None


class AppointmentResponse(BaseModel):
    id: int
    patient_id: int
    patient_name: str
    doctor_id: int
    doctor_name: str
    doctor_specialty: Optional[str]
    appointment_date: str
    status: str
    reason: str
    notes: Optional[str]
    created_at: str

    class Config:
        from_attributes = True


# ===== ENDPOINTS =====

@router.post("/create")
def create_appointment(
    patient_id: int,
    appointment: AppointmentCreate,
    db: Session = Depends(get_db)
):
    """Créer un nouveau rendez-vous (patient)"""
    
    # Vérifier que le patient existe
    patient = db.query(User).filter(User.id == patient_id).first()
    if not patient:
        return {"success": False, "message": "Patient non trouvé"}
    
    # Vérifier que le médecin existe
    doctor = db.query(User).filter(User.id == appointment.doctor_id, User.role == "doctor").first()
    if not doctor:
        return {"success": False, "message": "Médecin non trouvé"}
    
    # Créer le rendez-vous
    new_appointment = Appointment(
        patient_id=patient_id,
        doctor_id=appointment.doctor_id,
        appointment_date=datetime.fromisoformat(appointment.appointment_date),
        status="pending",
        reason=appointment.reason,
        notes=appointment.notes,
        created_at=datetime.now(),
        updated_at=datetime.now()
    )
    
    db.add(new_appointment)
    db.commit()
    db.refresh(new_appointment)
    
    return {
        "success": True,
        "message": "Rendez-vous créé avec succès",
        "appointment_id": new_appointment.id
    }


@router.get("/patient/{patient_id}")
def get_patient_appointments(
    patient_id: int,
    status: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Récupérer les rendez-vous d'un patient"""
    
    query = db.query(Appointment).filter(Appointment.patient_id == patient_id)
    
    if status:
        query = query.filter(Appointment.status == status)
    
    appointments = query.order_by(Appointment.appointment_date.desc()).all()
    
    result = []
    for apt in appointments:
        doctor = db.query(User).filter(User.id == apt.doctor_id).first()
        result.append({
            "id": apt.id,
            "patient_id": apt.patient_id,
            "patient_name": db.query(User).filter(User.id == apt.patient_id).first().name,
            "doctor_id": apt.doctor_id,
            "doctor_name": doctor.name,
            "doctor_specialty": getattr(doctor, 'specialty', 'Généraliste'),
            "appointment_date": apt.appointment_date.isoformat(),
            "status": apt.status,
            "reason": apt.reason,
            "notes": apt.notes,
            "created_at": apt.created_at.isoformat() if apt.created_at else None
        })
    
    return {
        "success": True,
        "count": len(result),
        "appointments": result
    }


@router.get("/doctor/{doctor_id}")
def get_doctor_appointments(
    doctor_id: int,
    status: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Récupérer les rendez-vous d'un médecin"""
    
    query = db.query(Appointment).filter(Appointment.doctor_id == doctor_id)
    
    if status:
        query = query.filter(Appointment.status == status)
    
    appointments = query.order_by(Appointment.appointment_date.desc()).all()
    
    result = []
    for apt in appointments:
        patient = db.query(User).filter(User.id == apt.patient_id).first()
        result.append({
            "id": apt.id,
            "patient_id": apt.patient_id,
            "patient_name": patient.name,
            "patient_email": patient.email,
            "doctor_id": apt.doctor_id,
            "doctor_name": db.query(User).filter(User.id == apt.doctor_id).first().name,
            "appointment_date": apt.appointment_date.isoformat(),
            "status": apt.status,
            "reason": apt.reason,
            "notes": apt.notes,
            "created_at": apt.created_at.isoformat() if apt.created_at else None
        })
    
    return {
        "success": True,
        "count": len(result),
        "appointments": result
    }


@router.put("/{appointment_id}/status")
def update_appointment_status(
    appointment_id: int,
    status: str,
    user_id: int,
    db: Session = Depends(get_db)
):
    """Mettre à jour le statut d'un rendez-vous (confirmer/refuser/annuler)"""
    
    appointment = db.query(Appointment).filter(Appointment.id == appointment_id).first()
    
    if not appointment:
        return {"success": False, "message": "Rendez-vous non trouvé"}
    
    # Vérifier les permissions
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        return {"success": False, "message": "Utilisateur non trouvé"}
    
    # Seul le médecin peut confirmer/refuser, le patient peut annuler
    if status in ["confirmed", "rejected"] and appointment.doctor_id != user_id:
        return {"success": False, "message": "Seul le médecin peut confirmer ou refuser"}
    
    if status == "cancelled" and appointment.patient_id != user_id:
        return {"success": False, "message": "Seul le patient peut annuler"}
    
    # Mettre à jour le statut
    appointment.status = status
    appointment.updated_at = datetime.now()
    db.commit()
    
    status_messages = {
        "confirmed": "Rendez-vous confirmé",
        "rejected": "Rendez-vous refusé",
        "cancelled": "Rendez-vous annulé",
        "completed": "Rendez-vous marqué comme terminé"
    }
    
    return {
        "success": True,
        "message": status_messages.get(status, "Statut mis à jour")
    }


@router.delete("/{appointment_id}")
def delete_appointment(
    appointment_id: int,
    user_id: int,
    db: Session = Depends(get_db)
):
    """Supprimer un rendez-vous"""
    
    appointment = db.query(Appointment).filter(Appointment.id == appointment_id).first()
    
    if not appointment:
        return {"success": False, "message": "Rendez-vous non trouvé"}
    
    # Seul le patient peut supprimer (ou le médecin si refusé)
    if appointment.patient_id != user_id and appointment.doctor_id != user_id:
        return {"success": False, "message": "Permission refusée"}
    
    db.delete(appointment)
    db.commit()
    
    return {
        "success": True,
        "message": "Rendez-vous supprimé"
    }


@router.get("/doctor/{doctor_id}/availability")
def get_doctor_availability(
    doctor_id: int,
    date: str,  # Format: "2025-11-15"
    db: Session = Depends(get_db)
):
    """Vérifier les créneaux disponibles pour un médecin à une date donnée"""
    
    # Heures de travail (9h-17h, créneaux de 30 min)
    working_hours = []
    for hour in range(9, 17):
        working_hours.append(f"{hour:02d}:00")
        working_hours.append(f"{hour:02d}:30")
    
    # Récupérer les rendez-vous existants pour cette date
    target_date = datetime.fromisoformat(date)
    appointments = db.query(Appointment).filter(
        Appointment.doctor_id == doctor_id,
        Appointment.appointment_date >= target_date,
        Appointment.appointment_date < datetime(target_date.year, target_date.month, target_date.day, 23, 59),
        Appointment.status.in_(["pending", "confirmed"])
    ).all()
    
    # Marquer les heures occupées
    booked_times = [apt.appointment_date.strftime("%H:%M") for apt in appointments]
    
    available_slots = []
    for time_slot in working_hours:
        available_slots.append({
            "time": time_slot,
            "available": time_slot not in booked_times
        })
    
    return {
        "success": True,
        "date": date,
        "slots": available_slots
    }