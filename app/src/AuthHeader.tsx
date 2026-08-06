import { Pressable, StyleSheet, Text, View } from 'react-native'

type AuthHeaderProps = {
  onBack: () => void
}

/**
 * 소셜 로그인 페이지 위에 얹는 뒤로가기 바.
 *
 * <p>카카오·네이버 로그인 화면은 우리가 만든 것이 아니라 빠져나올 수단이 없다.
 * iOS 는 웹뷰에 기본 뒤로가기가 없어 앱을 죽이는 것 말고는 방법이 없고, 안드로이드는
 * OS 뒤로가기가 있지만 화면상 안내가 없다. 다른 로그인 방법을 고르려면 이 바가 필요하다.
 *
 * <p>우리 웹 화면에서는 나타나지 않는다 — 거기엔 화면 자체의 뒤로가기가 있다.
 */
export function AuthHeader({ onBack }: AuthHeaderProps) {
  return (
    <View style={styles.bar}>
      <Pressable
        onPress={onBack}
        style={({ pressed }) => [styles.button, pressed && styles.buttonPressed]}
        // 아이콘만 있는 버튼이라 스크린리더가 읽을 이름을 직접 준다.
        accessibilityRole="button"
        accessibilityLabel="로그인 취소하고 돌아가기"
        // 34pt 버튼은 손가락보다 작다. 눌리는 범위를 넓힌다.
        hitSlop={12}
      >
        <Text style={styles.chevron}>‹</Text>
      </Pressable>
      <Text style={styles.title}>로그인</Text>
    </View>
  )
}

// 웹의 디자인 토큰(fe/src/styles/tokens.css)과 같은 값을 쓴다.
const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    height: 48,
    paddingHorizontal: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#e3e9e0',
    backgroundColor: '#fff',
  },
  button: {
    width: 34,
    height: 34,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 999,
  },
  buttonPressed: {
    backgroundColor: '#eef3ec',
  },
  chevron: {
    // '‹' 는 글꼴 기준선 때문에 그대로 두면 아래로 치우친다.
    lineHeight: 30,
    fontSize: 28,
    fontWeight: '400',
    color: '#1a2420',
  },
  title: {
    fontSize: 16,
    fontWeight: '700',
    color: '#3d4a44',
  },
})
