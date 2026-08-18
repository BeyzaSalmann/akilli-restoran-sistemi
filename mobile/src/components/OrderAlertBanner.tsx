import { Pressable, StyleSheet, Text, View } from 'react-native';

type Props = {
  title: string;
  body: string;
  variant?: 'order' | 'emotion';
  onDismiss: () => void;
  onPress?: () => void;
};

export default function OrderAlertBanner({
  title,
  body,
  variant = 'order',
  onDismiss,
  onPress,
}: Props) {
  const isEmotion = variant === 'emotion';
  return (
    <View style={[styles.wrap, isEmotion && styles.wrapEmotion]}>
      <Pressable style={styles.main} onPress={onPress} disabled={!onPress}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.body}>{body}</Text>
        {onPress ? <Text style={styles.hint}>Detay için dokun →</Text> : null}
      </Pressable>
      <Pressable onPress={onDismiss} hitSlop={12} style={styles.closeBtn}>
        <Text style={styles.close}>✕</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    position: 'absolute',
    top: 48,
    left: 12,
    right: 12,
    zIndex: 100,
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: '#1a73e8',
    borderRadius: 12,
    padding: 14,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 6,
  },
  wrapEmotion: {
    backgroundColor: '#e65100',
  },
  main: { flex: 1, paddingRight: 8 },
  title: { color: '#fff', fontWeight: '700', fontSize: 16, marginBottom: 4 },
  body: { color: '#e8f0fe', fontSize: 14 },
  hint: { color: '#cce0ff', fontSize: 12, marginTop: 6, fontWeight: '600' },
  closeBtn: { paddingHorizontal: 4 },
  close: { color: '#fff', fontSize: 18, fontWeight: '600' },
});
