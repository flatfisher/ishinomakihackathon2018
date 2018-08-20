import * as firebase from 'firebase/app';
import '@firebase/firestore';
import cert from '@/scripts/firebase/certification';

const firebaseApp = firebase.initializeApp({
    apiKey: cert.apiKey,
    authDomain: cert.authDomain,
    databaseURL: cert.databaseURL,
    projectId: cert.projectId,
    storageBucket: cert.storageBucket,
    messagingSenderId: cert.messagingSenderId
});

const db = firebaseApp.firestore();
db.settings({timestampsInSnapshots: true});

export { db };
export default firebaseApp;
