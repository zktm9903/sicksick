import { Pressable, StyleSheet, Text, View } from 'react-native'

type ErrorScreenProps = {
  onRetry: () => void
}

/**
 * 웹을 불러오지 못했을 때.
 *
 * 이게 없으면 서버가 죽었거나 네트워크가 끊겼을 때 흰 화면만 남아, 사용자는 앱이
 * 고장난 것으로 받아들인다.
 */
export function ErrorScreen({ onRetry }: ErrorScreenProps) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>연결할 수 없어요</Text>
      <Text style={styles.description}>
        인터넷 연결을 확인하고{'\n'}다시 시도해 주세요.
      </Text>
      <Pressable
        style={({ pressed }) => [styles.button, pressed && styles.buttonPressed]}
        onPress={onRetry}
      >
        <Text style={styles.buttonLabel}>다시 시도</Text>
      </Pressable>
    </View>
  )
}

// 웹의 디자인 토큰(fe/src/styles/tokens.css)과 같은 값을 쓴다.
const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 28,
    backgroundColor: '#f4f8f3',
  },
  title: {
    fontSize: 20,
    fontWeight: '800',
    color: '#1a2420',
  },
  description: {
    marginTop: 8,
    fontSize: 14.5,
    lineHeight: 23,
    color: '#6e7a74',
    textAlign: 'center',
  },
  button: {
    marginTop: 28,
    height: 52,
    justifyContent: 'center',
    paddingHorizontal: 40,
    borderRadius: 999,
    backgroundColor: '#5bad7f',
  },
  buttonPressed: {
    backgroundColor: '#4a9770',
  },
  buttonLabel: {
    fontSize: 16,
    fontWeight: '700',
    color: '#fff',
  },
})
