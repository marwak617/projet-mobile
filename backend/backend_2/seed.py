# backend/seed_database.py
"""
Script pour remplir la base de données avec des données de test
"""

from faker import Faker
from sqlalchemy.orm import Session
from database import SessionLocal, engine, Base
from models import User, Doctor, Appointment, MedicalDocument
from datetime import datetime, timedelta
import random
import os

# Créer les tables si elles n'existent pas
Base.metadata.create_all(bind=engine)

# Initialiser Faker avec localisation française
fake = Faker('fr_FR')

# Créer le dossier pour les documents si nécessaire
UPLOAD_DIR = "uploads/medical_documents"
os.makedirs(UPLOAD_DIR, exist_ok=True)


def clear_database(db: Session):
    """Vider complètement la base de données"""
    print("🗑️  Suppression des données existantes...")
    db.query(Appointment).delete()
    db.query(MedicalDocument).delete()
    db.query(Doctor).delete()
    db.query(User).delete()
    db.commit()
    print("✅ Base de données vidée")


def seed_users(db: Session, n=10):
    """Créer des utilisateurs de test"""
    users = []
    
    # Créer un utilisateur de test avec identifiants connus
    test_user = User(
        name="Test User",
        email="test@test.com",
        password="test123",
        region="Casablanca",
        role="patient",
        phone="+212 6 12 34 56 78",
        address="123 Rue Test, Casablanca"
    )
    users.append(test_user)
    db.add(test_user)
    
    # Créer des utilisateurs aléatoires
    for _ in range(n - 1):
        user = User(
            name=fake.name(),
            email=fake.unique.email(),
            password="password123",
            region=random.choice([
                "Casablanca", "Rabat", "Marrakech", "Fès", 
                "Tanger", "Agadir", "Meknès", "Oujda"
            ]),
            role="patient",
            phone=fake.phone_number(),
            address=fake.address()
        )
        users.append(user)
        db.add(user)
    
    db.commit()
    
    # Rafraîchir pour obtenir les IDs
    for user in users:
        db.refresh(user)
    
    print(f"✅ {len(users)} utilisateurs ajoutés!")
    print(f"   📧 Compte de test: test@test.com / test123")
    return users


def seed_doctors(db: Session, n=10):
    """Créer des médecins de test"""
    doctors = []
    
    specialities = [
        "Cardiologue", "Dentiste", "Dermatologue", "Généraliste", 
        "Pédiatre", "Ophtalmologue", "Gynécologue", "ORL",
        "Psychiatre", "Radiologue"
    ]
    
    moroccan_cities = [
        ("Casablanca", 33.5731, -7.5898),
        ("Rabat", 34.0209, -6.8416),
        ("Marrakech", 31.6295, -7.9811),
        ("Fès", 34.0181, -5.0078),
        ("Tanger", 35.7595, -5.8340),
        ("Agadir", 30.4278, -9.5981),
        ("Meknès", 33.8935, -5.5473),
        ("Oujda", 34.6814, -1.9086)
    ]
    
    for _ in range(n):
        city, lat, lon = random.choice(moroccan_cities)
        doctor = Doctor(
            name=f"Dr. {fake.last_name()} {fake.first_name()}",
            speciality=random.choice(specialities),
            city=city,
            latitude=str(lat + random.uniform(-0.1, 0.1)),
            longitude=str(lon + random.uniform(-0.1, 0.1))
        )
        doctors.append(doctor)
        db.add(doctor)
    
    db.commit()
    
    # Rafraîchir pour obtenir les IDs
    for doctor in doctors:
        db.refresh(doctor)
    
    print(f"✅ {len(doctors)} médecins ajoutés!")
    return doctors


def seed_appointments(db: Session, users, doctors, n=20):
    """Créer des rendez-vous de test"""
    appointments = []
    
    for _ in range(n):
        # Date aléatoire entre -30 jours et +60 jours
        days_offset = random.randint(-30, 60)
        appointment_date = datetime.now() + timedelta(days=days_offset)
        
        # Statut en fonction de la date
        if days_offset < 0:
            status = random.choice(["confirmed", "cancelled", "completed"])
        else:
            status = random.choice(["pending", "confirmed"])
        
        appointment = Appointment(
            user_id=random.choice(users).id,
            doctor_id=random.choice(doctors).id,
            date=appointment_date,
            status=status
        )
        appointments.append(appointment)
        db.add(appointment)
    
    db.commit()
    print(f"✅ {len(appointments)} rendez-vous ajoutés!")
    return appointments


def seed_medical_documents(db: Session, users, n=15):
    """Créer des documents médicaux de test (métadonnées seulement)"""
    documents = []
    
    document_types = ["mutuelle", "ordonnance", "analyse", "radio", "autre"]
    file_extensions = {
        "mutuelle": [".pdf", ".jpg"],
        "ordonnance": [".pdf", ".jpg"],
        "analyse": [".pdf"],
        "radio": [".jpg", ".png", ".dcm"],
        "autre": [".pdf", ".jpg", ".doc"]
    }
    
    mime_types = {
        ".pdf": "application/pdf",
        ".jpg": "image/jpeg",
        ".png": "image/png",
        ".dcm": "application/dicom",
        ".doc": "application/msword"
    }
    
    for _ in range(n):
        user = random.choice(users)
        doc_type = random.choice(document_types)
        extension = random.choice(file_extensions[doc_type])
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        random_suffix = random.randint(1000, 9999)
        filename = f"{user.id}_{doc_type}_{timestamp}_{random_suffix}{extension}"
        
        # Créer un fichier vide pour la démo (optionnel)
        file_path = os.path.join(UPLOAD_DIR, filename)
        with open(file_path, 'w') as f:
            f.write(f"Document de test - {doc_type}")
        
        file_size = os.path.getsize(file_path)
        
        document = MedicalDocument(
            user_id=user.id,
            filename=filename,
            original_filename=f"{doc_type}_{fake.word()}{extension}",
            file_type=doc_type,
            file_size=file_size,
            mime_type=mime_types.get(extension, "application/octet-stream"),
            upload_date=datetime.now() - timedelta(days=random.randint(0, 90))
        )
        documents.append(document)
        db.add(document)
    
    db.commit()
    print(f"✅ {len(documents)} documents médicaux ajoutés!")
    return documents


def display_summary(db: Session):
    """Afficher un résumé des données"""
    print("\n" + "="*60)
    print("📊 RÉSUMÉ DE LA BASE DE DONNÉES")
    print("="*60)
    
    user_count = db.query(User).count()
    doctor_count = db.query(Doctor).count()
    appointment_count = db.query(Appointment).count()
    document_count = db.query(MedicalDocument).count()
    
    print(f"👥 Utilisateurs: {user_count}")
    print(f"⚕️  Médecins: {doctor_count}")
    print(f"📅 Rendez-vous: {appointment_count}")
    print(f"📄 Documents: {document_count}")
    
    print("\n📌 Informations de connexion:")
    print("   Email: test@test.com")
    print("   Mot de passe: test123")
    print("="*60 + "\n")


def main():
    db = SessionLocal()
    
    try:
        print("🚀 Démarrage du remplissage de la base de données...")
        print()
        
        # Demander confirmation pour vider la base
        response = input("⚠️  Voulez-vous vider la base de données avant? (oui/non): ")
        if response.lower() in ['oui', 'yes', 'o', 'y']:
            clear_database(db)
            print()
        
        # Remplir la base de données
        users = seed_users(db, n=15)
        doctors = seed_doctors(db, n=12)
        appointments = seed_appointments(db, users, doctors, n=30)
        documents = seed_medical_documents(db, users, n=20)
        
        # Afficher le résumé
        display_summary(db)
        
        print("🎉 Données de test insérées avec succès!")
        
    except Exception as e:
        print(f"❌ Erreur: {e}")
        db.rollback()
    finally:
        db.close()


if __name__ == "__main__":
    main()