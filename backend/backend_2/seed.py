# backend/seed_test_data.py

from sqlalchemy.orm import Session
from database import SessionLocal, engine, Base
from models import User, Appointment
from datetime import datetime, timedelta
import random

def create_tables():
    """Créer toutes les tables"""
    print("📦 Création des tables...")
    Base.metadata.create_all(bind=engine)
    print("✅ Tables créées avec succès!")

def clear_data(db: Session):
    """Supprimer toutes les données existantes"""
    print("🗑️  Suppression des données existantes...")
    db.query(Appointment).delete()
    db.query(User).delete()
    db.commit()
    print("✅ Données supprimées!")

def create_users(db: Session):
    """Créer des utilisateurs de test"""
    print("\n👥 Création des utilisateurs...")
    
    # Patients
    patients = [
        {
            "name": "Nicolas Dumas",
            "email": "ndumas@example.org",
            "password": "password123",
            "role": "patient",
            "region": "Casablanca",
            "phone": "+212 6 12 34 56 78",
            "address": "123 Rue Mohammed V, Casablanca"
        },
        {
            "name": "Fatima Alaoui",
            "email": "falaoui@patient.ma",
            "password": "password123",
            "role": "patient",
            "region": "Rabat",
            "phone": "+212 6 23 45 67 89",
            "address": "45 Avenue Hassan II, Rabat"
        },
        {
            "name": "Ahmed Benali",
            "email": "abenali@patient.ma",
            "password": "password123",
            "role": "patient",
            "region": "Marrakech",
            "phone": "+212 6 34 56 78 90",
            "address": "78 Rue de la Liberté, Marrakech"
        },
        {
            "name": "Leila Mansouri",
            "email": "lmansouri@patient.ma",
            "password": "password123",
            "role": "patient",
            "region": "Fès",
            "phone": "+212 6 45 67 89 01",
            "address": "12 Boulevard Zerktouni, Fès"
        }
    ]
    
    # Médecins
    doctors = [
        {
            "name": "Hassan Bennani",
            "email": "hbennani@doctor.ma",
            "password": "doctor123",
            "role": "doctor",
            "specialty": "Cardiologue",
            "region": "Casablanca",
            "phone": "+212 5 22 12 34 56",
            "address": "Clinique Al Amal, Bd Anfa, Casablanca"
        },
        {
            "name": "Samira Tazi",
            "email": "stazi@doctor.ma",
            "password": "doctor123",
            "role": "doctor",
            "specialty": "Pédiatre",
            "region": "Rabat",
            "phone": "+212 5 37 23 45 67",
            "address": "Cabinet Médical, Agdal, Rabat"
        },
        {
            "name": "Youssef Idrissi",
            "email": "yidrissi@doctor.ma",
            "password": "doctor123",
            "role": "doctor",
            "specialty": "Dentiste",
            "region": "Marrakech",
            "phone": "+212 5 24 34 56 78",
            "address": "Centre Dentaire, Guéliz, Marrakech"
        },
        {
            "name": "Khadija El Amrani",
            "email": "kelamrani@doctor.ma",
            "password": "doctor123",
            "role": "doctor",
            "specialty": "Dermatologue",
            "region": "Fès",
            "phone": "+212 5 35 45 67 89",
            "address": "Polyclinique Atlas, Fès"
        },
        {
            "name": "Mohamed Chakir",
            "email": "mchakir@doctor.ma",
            "password": "doctor123",
            "role": "doctor",
            "specialty": "Généraliste",
            "region": "Casablanca",
            "phone": "+212 5 22 56 78 90",
            "address": "Cabinet Médical, Maarif, Casablanca"
        },
        {
            "name": "Nadia Berrada",
            "email": "nberrada@doctor.ma",
            "password": "doctor123",
            "role": "doctor",
            "specialty": "Gynécologue",
            "region": "Rabat",
            "phone": "+212 5 37 67 89 01",
            "address": "Clinique de la Femme, Hassan, Rabat"
        },
        {
            "name": "Rachid Amrani",
            "email": "ramrani@doctor.ma",
            "password": "doctor123",
            "role": "doctor",
            "specialty": "Ophtalmologue",
            "region": "Tanger",
            "phone": "+212 5 39 78 90 12",
            "address": "Centre Ophtalmologique, Tanger"
        }
    ]
    
    created_users = []
    
    # Créer les patients
    for patient_data in patients:
        user = User(**patient_data)
        db.add(user)
        db.flush()  # Pour obtenir l'ID
        created_users.append(user)
        print(f"   ✅ Patient créé: {user.name} (ID: {user.id})")
    
    # Créer les médecins
    for doctor_data in doctors:
        user = User(**doctor_data)
        db.add(user)
        db.flush()
        created_users.append(user)
        print(f"   ✅ Médecin créé: Dr. {user.name} - {user.specialty} (ID: {user.id})")
    
    db.commit()
    return created_users

def create_appointments(db: Session, users):
    """Créer des rendez-vous de test"""
    print("\n📅 Création des rendez-vous...")
    
    # Séparer patients et médecins
    patients = [u for u in users if u.role == "patient"]
    doctors = [u for u in users if u.role == "doctor"]
    
    statuses = ["pending", "confirmed", "rejected", "cancelled", "completed"]
    reasons = [
        "Consultation générale",
        "Contrôle de routine",
        "Douleurs thoraciques",
        "Check-up annuel",
        "Problème de peau",
        "Mal de dents",
        "Vaccination",
        "Suivi post-opératoire",
        "Consultation pédiatrique",
        "Examen de la vue"
    ]
    
    appointments_count = 0
    
    # Créer des rendez-vous pour chaque patient
    for patient in patients:
        # 2-4 rendez-vous par patient
        num_appointments = random.randint(2, 4)
        
        for i in range(num_appointments):
            doctor = random.choice(doctors)
            
            # Dates variées (passé, présent, futur)
            days_offset = random.randint(-30, 60)  # De -30 jours à +60 jours
            hours = random.choice([9, 10, 11, 14, 15, 16, 17])
            minutes = random.choice([0, 30])
            
            appointment_date = datetime.now() + timedelta(
                days=days_offset,
                hours=hours - datetime.now().hour,
                minutes=minutes - datetime.now().minute
            )
            
            # Statut selon la date
            if days_offset < -7:
                status = "completed"
            elif days_offset < 0:
                status = random.choice(["completed", "cancelled"])
            elif days_offset < 2:
                status = random.choice(["pending", "confirmed"])
            else:
                status = random.choice(["pending", "confirmed", "rejected"])
            
            appointment = Appointment(
                patient_id=patient.id,
                doctor_id=doctor.id,
                appointment_date=appointment_date,
                status=status,
                reason=random.choice(reasons),
                notes=f"Notes pour le rendez-vous #{appointments_count + 1}" if random.random() > 0.5 else None,
                created_at=datetime.now() - timedelta(days=abs(days_offset) + 1),
                updated_at=datetime.now()
            )
            
            db.add(appointment)
            appointments_count += 1
            
            status_emoji = {
                "pending": "⏳",
                "confirmed": "✅",
                "rejected": "❌",
                "cancelled": "🚫",
                "completed": "✔️"
            }
            
            print(f"   {status_emoji.get(status, '📅')} RDV: {patient.name} → Dr. {doctor.name} "
                  f"({appointment_date.strftime('%d/%m/%Y %H:%M')}) [{status}]")
    
    db.commit()
    print(f"\n✅ {appointments_count} rendez-vous créés!")

def main():
    """Fonction principale"""
    print("=" * 60)
    print("🚀 INITIALISATION DE LA BASE DE DONNÉES")
    print("=" * 60)
    
    # Créer les tables
    create_tables()
    
    # Créer une session
    db = SessionLocal()
    
    try:
        # Supprimer les anciennes données
        clear_data(db)
        
        # Créer les utilisateurs
        users = create_users(db)
        
        # Créer les rendez-vous
        create_appointments(db, users)
        
        print("\n" + "=" * 60)
        print("✨ BASE DE DONNÉES INITIALISÉE AVEC SUCCÈS!")
        print("=" * 60)
        print("\n📊 RÉSUMÉ:")
        print(f"   👥 Patients: {len([u for u in users if u.role == 'patient'])}")
        print(f"   👨‍⚕️ Médecins: {len([u for u in users if u.role == 'doctor'])}")
        print(f"   📅 Rendez-vous: {db.query(Appointment).count()}")
        
        print("\n🔑 IDENTIFIANTS DE TEST:")
        print("\n   📱 PATIENT:")
        print("      Email: ndumas@example.org")
        print("      Password: password123")
        print("\n   👨‍⚕️ MÉDECIN:")
        print("      Email: hbennani@doctor.ma")
        print("      Password: doctor123")
        
    except Exception as e:
        print(f"\n❌ ERREUR: {e}")
        db.rollback()
        raise
    finally:
        db.close()

if __name__ == "__main__":
    main()