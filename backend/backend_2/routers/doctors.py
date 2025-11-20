from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from database import get_db
import crud, schemas
from models import User
from typing import Optional

router = APIRouter(prefix="/doctors", tags=["doctors"])

@router.post("/", response_model=schemas.DoctorResponse)
def create_doctor(doctor: schemas.DoctorCreate, db: Session = Depends(get_db)):
    return crud.create_doctor(db, doctor.dict())

@router.get("/")
def get_doctors(
    db: Session = Depends(get_db), 
    specialty: Optional[str] = Query(None),
    skip: int = Query(0),
    limit: int = Query(100)
):
    """Récupérer la liste des médecins avec filtrage optionnel par spécialité"""
    
    # Construction de la requête de base
    query = db.query(User).filter(User.role == "doctor")
    
    # Filtrer par spécialité si fournie et différente de "Toutes les spécialités"
    if specialty and specialty != "Toutes les spécialités":
        query = query.filter(User.specialty == specialty)
    
    # Appliquer pagination
    doctors = query.offset(skip).limit(limit).all()
    
    return {
        "success": True,
        "count": len(doctors),
        "doctors": [
            {
                "id": doc.id,
                "name": doc.name,
                "email": doc.email,
                "specialty": getattr(doc, 'specialty', 'Généraliste'),
                "region": getattr(doc, 'region', None),
                "phone": getattr(doc, 'phone', None),
                "address": getattr(doc, 'address', None)
            } for doc in doctors
        ]
    }