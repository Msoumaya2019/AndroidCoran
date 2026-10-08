# Proposition FCM — aucune modification appliquée

La référence `src/services/notifications.ts` enregistre des jetons Expo via les RPC et tables du projet partagé. Un jeton FCM natif ne peut pas être traité comme un jeton Expo. Ne pas réutiliser la même ligne d'appareil ni remplacer les appareils iPhone.

Étapes nécessaires avant implémentation validée :

1. Fournir une configuration Firebase Android correspondant à `com.msoumaya.androidcoran`, et configurer les credentials FCM côté serveur hors Git.
2. Auditer les RPC réellement déployées et les clés d'identification des appareils, sans modifier l'existant.
3. Proposer un stockage additif des appareils FCM avec `user_id`, `installation_id` unique par installation Android, fournisseur `fcm`, jeton, préférences et horodatages. Les appareils Expo et iOS restent distincts.
4. Ajouter une route d'envoi FCM réservée au serveur, reprenant les types de notifications et leurs données de navigation de la référence. Déduplication par événement, renouvellement des jetons et retrait d'une seule installation à la déconnexion.
5. Soumettre le SQL et la fonction serveur précis à l'utilisateur pour autorisation **avant** de les appliquer.
6. Tester deux appareils iPhone et Android connectés au même utilisateur, ouverture vers conversation/récitation/quiz/programme, préférences, révocation et rotation du jeton.

Cette proposition ne contient aucun SQL exécutable et ne change aucun service partagé. Les notifications distantes restent **Bloqué** jusqu'à la configuration et l'autorisation nécessaires.
