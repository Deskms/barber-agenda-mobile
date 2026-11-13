import { StyleSheet, Text, View } from 'react-native';

// Pantalla inicial provisional. La consulta de disponibilidad y la reserva
// se conectarán a la API de barber-agenda-web en una entrega posterior.
export default function InicioScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.titulo}>Barber Agenda</Text>
      <Text style={styles.subtitulo}>Joss Estética y Barbería</Text>
      <Text style={styles.nota}>Próximamente: consulta de horarios y reservas.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', alignItems: 'center', justifyContent: 'center', padding: 24 },
  titulo: { fontSize: 28, fontWeight: '700', color: '#1F3A5F' },
  subtitulo: { fontSize: 16, marginTop: 4, color: '#444' },
  nota: { fontSize: 14, marginTop: 24, color: '#777', textAlign: 'center' },
});
