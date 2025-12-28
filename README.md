1️⃣ Ajouter ta copine comme collaboratrice sur GitHub

Va sur ton dépôt GitHub :
https://github.com/marwak617/projet-mobile

Clique sur Settings → Collaborators and Teams (ou Manage access selon la version)

Clique sur Invite a collaborator

Entre le nom d’utilisateur GitHub de ta copine, puis clique sur Add

Elle recevra une invitation par mail. Une fois acceptée, elle aura accès au dépôt pour cloner, push et pull.

2️⃣ Ta copine clone le projet sur son PC

Elle doit ouvrir un terminal et faire :

git clone https://github.com/marwak617/projet-mobile.git


Elle aura alors tout le projet localement.

3️⃣ Workflow collaboratif recommandé
Créer des branches pour travailler

Pour éviter les conflits, ne travaillez jamais directement sur main

Exemple :

git checkout -b feature/login


Elle fait ses modifications sur cette branche :

git add .
git commit -m "Ajout page login"
git push origin feature/login

Pull request

Une fois terminée, elle peut créer une pull request (PR) depuis GitHub vers main

Tu peux relire et valider la PR avant de fusionner

Cela évite de casser le projet

4️⃣ Pour rester à jour

Chaque collaborateur doit récupérer les changements de main régulièrement :

git checkout main
git pull origin main


💡 Astuce pour éviter les conflits Android Studio / backend

Si chacun travaille sur une partie spécifique (backend ou frontend), créez des branches claires :
backend-dev, frontend-dev, etc.


la branche backend-dev

dans cmd : studio64.exe "C:\Users\net\projet-mobile\frontend"
uvicorn main:app --reload --host 0.0.0.0




tables pour gérer les conversations:

-- Créer les tables de chat (compatible avec votre structure existante)

CREATE TABLE IF NOT EXISTS conversations (
    id SERIAL PRIMARY KEY,
    patient_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    medecin_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_message_at TIMESTAMP,
    CONSTRAINT unique_conversation UNIQUE (patient_id, medecin_id)
);

CREATE INDEX idx_conversations_patient ON conversations(patient_id);
CREATE INDEX idx_conversations_medecin ON conversations(medecin_id);
CREATE INDEX idx_conversations_last_message ON conversations(last_message_at DESC);

CREATE TABLE IF NOT EXISTS messages (
    id SERIAL PRIMARY KEY,
    conversation_id INTEGER NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    message_type VARCHAR(20) DEFAULT 'text' NOT NULL,
    file_url VARCHAR(500),
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_created ON messages(created_at DESC);
CREATE INDEX idx_messages_unread ON messages(conversation_id, is_read) WHERE is_read = FALSE;

CREATE TABLE IF NOT EXISTS chat_attachments (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_attachments_message ON chat_attachments(message_id);

CREATE TABLE IF NOT EXISTS message_read_status (
    id SERIAL PRIMARY KEY,
    message_id INTEGER NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_message_read UNIQUE (message_id, user_id)
);

CREATE INDEX idx_read_status_message ON message_read_status(message_id);
CREATE INDEX idx_read_status_user ON message_read_status(user_id);

CREATE TABLE IF NOT EXISTS chat_notifications (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_id INTEGER REFERENCES messages(id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_notifications_user ON chat_notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON chat_notifications(created_at DESC);

-- Trigger pour mettre à jour last_message_at automatiquement
CREATE OR REPLACE FUNCTION update_conversation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations 
    SET last_message_at = NEW.created_at 
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_conversation_timestamp
AFTER INSERT ON messages
FOR EACH ROW
EXECUTE FUNCTION update_conversation_timestamp();
