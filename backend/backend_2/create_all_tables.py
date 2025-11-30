# backend/create_all_tables.py
"""
Script pour créer toutes les tables de la base de données
Exécutez ce script une fois pour initialiser la base de données
"""

from database import engine, Base
from models import User, MedicalDocument, Doctor, Appointment
import os

def drop_all_tables():
    """Supprimer toutes les tables (ATTENTION: perte de données)"""
    print("⚠️  Suppression de toutes les tables...")
    Base.metadata.drop_all(bind=engine)
    print("✅ Tables supprimées")

def create_all_tables():
    """Créer toutes les tables définies dans models.py"""
    print("📦 Création des tables...")
    Base.metadata.create_all(bind=engine)
    print("\n✅ Toutes les tables ont été créées avec succès!\n")
    print("📋 Tables créées:")
    print("  ✓ users")
    print("  ✓ medical_documents")
    print("  ✓ doctors")
    print("  ✓ appointments")
    print("\n🎉 Base de données prête à l'emploi!")

def reset_database():
    """Réinitialiser complètement la base de données"""
    response = input("\n⚠️  ATTENTION: Ceci va supprimer TOUTES les données. Continuer? (oui/non): ")
    if response.lower() in ['oui', 'yes', 'o', 'y']:
        drop_all_tables()
        create_all_tables()
    else:
        print("❌ Opération annulée")

if __name__ == "__main__":
    print("=" * 60)
    print("🗄️  GESTIONNAIRE DE BASE DE DONNÉES")
    print("=" * 60)
    print("\nOptions:")
    print("1. Créer les tables (conserve les données existantes)")
    print("2. Réinitialiser la base de données (SUPPRIME TOUT)")
    print("3. Quitter")
    
    choice = input("\nVotre choix (1-3): ")
    
    if choice == "1":
        create_all_tables()
    elif choice == "2":
        reset_database()
    elif choice == "3":
        print("👋 Au revoir!")
    else:
        print("❌ Choix invalide")