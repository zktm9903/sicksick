import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { Checkbox, CheckboxChevron, RequirementTag } from '@/components/ui/Checkbox'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { SESSION_QUERY_KEY } from '@/features/auth/session'
import { agreeTerms, fetchTerms, type Term } from '@/features/signup/api'
import { alertError } from '@/lib/alertError'

import styles from './TermsPage.module.css'

export function TermsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: terms = [] } = useQuery({ queryKey: ['terms'], queryFn: fetchTerms })
  const [agreed, setAgreed] = useState<Record<string, boolean>>({})

  const requiredCodes = useMemo(
    () => terms.filter((term) => term.required).map((term) => term.code),
    [terms],
  )
  const allRequiredChecked =
    requiredCodes.length > 0 && requiredCodes.every((code) => agreed[code])
  const allChecked = terms.length > 0 && terms.every((term) => agreed[term.code])

  const toggleAll = (checked: boolean) => {
    setAgreed(Object.fromEntries(terms.map((term) => [term.code, checked])))
  }

  const toggle = (code: string, checked: boolean) => {
    setAgreed((prev) => ({ ...prev, [code]: checked }))
  }

  const submit = useMutation({
    // 선택 약관의 거부도 그대로 보낸다. 서버가 거부 이력까지 기록한다.
    mutationFn: () =>
      agreeTerms(
        Object.fromEntries(terms.map((term) => [term.code, agreed[term.code] ?? false])),
      ),
    onSuccess: async (result) => {
      // nextStep 이 바뀌었으므로 세션을 다시 읽어야 AuthGuard 가 통과시킨다.
      await queryClient.invalidateQueries({ queryKey: SESSION_QUERY_KEY })
      navigate(result.nextStep)
    },
    onError: (error) => alertError(error, '동의 저장에 실패했어요.'),
  })

  return (
    <Screen>
      <AppHeader title="약관 동의" onBack={() => navigate(ROUTES.login)} />
      <ProgressDots step="terms" />

      <ScreenBody className={styles.body}>
        <h2 className={styles.title}>
          약관에 동의 하고
          <br />
          인증을 진행해 주세요
        </h2>

        <Checkbox className={styles.all} checked={allChecked} onChange={toggleAll} boxed strong>
          전체 동의
        </Checkbox>

        {terms.map((term: Term) => (
          <Checkbox
            key={term.code}
            checked={agreed[term.code] ?? false}
            onChange={(checked) => toggle(term.code, checked)}
            divided
            trailing={<CheckboxChevron />}
          >
            {term.title} <RequirementTag required={term.required} />
          </Checkbox>
        ))}
      </ScreenBody>

      <ScreenFooter>
        <Button
          disabled={!allRequiredChecked || submit.isPending}
          onClick={() => submit.mutate()}
        >
          본인 인증하기
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
