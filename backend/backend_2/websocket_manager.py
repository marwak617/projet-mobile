from fastapi import WebSocket
from typing import Dict, List
import json
import asyncio

class ConnectionManager:
    def __init__(self):
        # user_id -> List[WebSocket]
        self.active_connections: Dict[int, List[WebSocket]] = {}
        # Lock pour éviter les race conditions
        self._lock = asyncio.Lock()

    async def connect(self, websocket: WebSocket, user_id: int):
        """Accepter une connexion WebSocket"""
        await websocket.accept()
        
        async with self._lock:
            if user_id not in self.active_connections:
                self.active_connections[user_id] = []
            self.active_connections[user_id].append(websocket)
        
        print(f"✅ User {user_id} connected. Total connections: {len(self.active_connections[user_id])}")

    async def disconnect(self, websocket: WebSocket, user_id: int):
        """Déconnecter un WebSocket"""
        async with self._lock:
            if user_id in self.active_connections:
                # ✅ FIX: Vérifier que le websocket existe avant de le retirer
                if websocket in self.active_connections[user_id]:
                    self.active_connections[user_id].remove(websocket)
                
                # Nettoyer si plus aucune connexion
                if not self.active_connections[user_id]:
                    del self.active_connections[user_id]
        
        print(f"❌ User {user_id} disconnected")
        
        # ✅ FIX: Fermer proprement le websocket
        try:
            await websocket.close()
        except Exception as e:
            print(f"⚠️ Error closing websocket for user {user_id}: {e}")

    async def send_personal_message(self, message: dict, user_id: int):
        """Envoyer un message à un utilisateur spécifique"""
        if user_id not in self.active_connections:
            print(f"⚠️ User {user_id} not connected")
            return
        
        # FIX: Copier la liste pour éviter les modifications pendant l'itération
        connections = list(self.active_connections.get(user_id, []))
        dead_connections = []
        
        for connection in connections:
            try:
                await connection.send_json(message)
                print(f"📤 Message sent to user {user_id}")
            except Exception as e:
                print(f"❌ Error sending to user {user_id}: {e}")
                dead_connections.append(connection)
        
        #FIX: Nettoyer les connexions mortes
        if dead_connections:
            async with self._lock:
                for dead_conn in dead_connections:
                    if user_id in self.active_connections and dead_conn in self.active_connections[user_id]:
                        self.active_connections[user_id].remove(dead_conn)
                        try:
                            await dead_conn.close()
                        except:
                            pass

    async def broadcast_to_conversation(self, message: dict, user_ids: List[int]):
        """Diffuser un message à plusieurs utilisateurs"""
        print(f"📢 Broadcasting to users: {user_ids}")
        
        tasks = []
        for user_id in user_ids:
            task = self.send_personal_message(message, user_id)
            tasks.append(task)
        
        #FIX: Envoyer en parallèle plutôt que séquentiellement
        await asyncio.gather(*tasks, return_exceptions=True)

    def get_active_users(self) -> List[int]:
        """Obtenir la liste des utilisateurs connectés"""
        return list(self.active_connections.keys())

    def get_user_connection_count(self, user_id: int) -> int:
        """Obtenir le nombre de connexions pour un utilisateur"""
        return len(self.active_connections.get(user_id, []))

    async def broadcast_to_all(self, message: dict):
        """Diffuser un message à tous les utilisateurs connectés"""
        all_user_ids = self.get_active_users()
        await self.broadcast_to_conversation(message, all_user_ids)

manager = ConnectionManager()