import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router'

import { ROUTES } from '@/app/routes'
import { AppHeader } from '@/components/layout/AppHeader'
import { Screen, ScreenBody, ScreenFooter } from '@/components/layout/Screen'
import { Button } from '@/components/ui/Button'
import { Chip } from '@/components/ui/Chip'
import { ProgressDots } from '@/components/ui/ProgressDots'
import { TextField } from '@/components/ui/TextField'
import { searchConditions } from '@/features/onboarding/api'
import { ONBOARDING_STEPS } from '@/features/onboarding/constants'
import { useOnboarding } from '@/features/onboarding/context'
import type { Condition, DraftCondition } from '@/features/onboarding/types'

import { CustomConditionForm, type CustomConditionDraft } from './CustomConditionForm'
import styles from './onboarding.module.css'
import conditionStyles from './ConditionPage.module.css'

export function ConditionPage() {
  const navigate = useNavigate()
  const { draft, patch } = useOnboarding()

  const [query, setQuery] = useState('')
  /** null 이면 폼이 닫힌 상태. 새 등록과 수정 모두 이 상태 하나로 다룬다. */
  const [customForm, setCustomForm] = useState<
    { mode: 'create' | 'edit'; initial: CustomConditionDraft } | null
  >(null)

  const trimmed = query.trim()
  const { data: results = [] } = useQuery({
    queryKey: ['conditions', trimmed],
    queryFn: () => searchConditions(trimmed),
    enabled: trimmed.length > 0,
  })

  const selected = draft.conditions
  const isSelected = (name: string) => selected.some((c) => c.name === name)

  /** 이전 목록에서 파생되므로 함수 형태로 갱신한다. */
  const updateConditions = (update: (prev: DraftCondition[]) => DraftCondition[]) =>
    patch((prev) => ({ conditions: update(prev.conditions) }))

  const addMaster = (condition: Condition) => {
    updateConditions((prev) =>
      prev.some((c) => c.name === condition.name)
        ? prev
        : [
            ...prev,
            {
              conditionId: condition.id,
              name: condition.name,
              code: condition.code,
              description: condition.description,
              status: 'DIAGNOSED',
              suggestedSymptoms: condition.symptoms,
            },
          ],
    )
    setQuery('')
  }

  const saveCustom = (custom: CustomConditionDraft) => {
    const entry: DraftCondition = {
      conditionId: null,
      name: custom.name,
      code: custom.code || null,
      description: custom.description || null,
      status: 'DIAGNOSED',
      // 직접 등록한 질환은 여기서 고른 증상이 곧 증상 화면의 후보가 된다.
      suggestedSymptoms: custom.symptoms
        .filter((s) => s.id !== null)
        .map((s) => ({ id: s.id as number, name: s.name })),
    }

    const isEdit = customForm?.mode === 'edit'
    updateConditions((prev) =>
      isEdit
        ? prev.map((c) => (c.name === custom.name ? { ...c, ...entry, status: c.status } : c))
        : [...prev, entry],
    )
    setCustomForm(null)
    setQuery('')
  }

  const notSelected = results.filter((r) => !isSelected(r.name))

  return (
    <Screen>
      <AppHeader title="프로필 설정" onBack={() => navigate(ROUTES.onboarding.conditionGate)} />
      <ProgressDots steps={ONBOARDING_STEPS} step="condition" label="프로필 설정 진행률" />

      <ScreenBody className={styles.body}>
        <h2 className={styles.title}>
          {draft.nickname.trim() ? `${draft.nickname.trim()}님, ` : ''}현재 관리하고 싶은
          <br />
          질환이 있나요?
        </h2>

        <TextField
          placeholder="질병명을 검색해보세요"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <p className={conditionStyles.hint}>여러 개 선택할 수 있어요</p>

        {trimmed && !customForm && (
          <div className={conditionStyles.results}>
            {notSelected.map((condition) => (
              <button
                key={condition.id}
                type="button"
                className={conditionStyles.resultItem}
                onClick={() => addMaster(condition)}
              >
                {condition.name}
              </button>
            ))}
            {/* 마스터에 없는 질환이 훨씬 많다. 검색 결과와 무관하게 항상 열어둔다. */}
            <button
              type="button"
              className={conditionStyles.customItem}
              onClick={() =>
                setCustomForm({
                  mode: 'create',
                  initial: { name: trimmed, code: '', description: '', symptoms: [] },
                })
              }
            >
              &lsquo;{trimmed}&rsquo; 직접 입력하기
            </button>
          </div>
        )}

        {customForm && (
          <CustomConditionForm
            mode={customForm.mode}
            initial={customForm.initial}
            onSubmit={saveCustom}
            onCancel={() => setCustomForm(null)}
          />
        )}

        {selected.length > 0 && (
          <div className={conditionStyles.selectedList}>
            <p className={conditionStyles.selectedCount}>{selected.length}개 선택됨</p>

            {selected.map((condition) => (
              <div key={condition.name} className={styles.card}>
                <div className={conditionStyles.cardHeader}>
                  <span className={styles.cardTitle}>{condition.name}</span>
                  <div className={conditionStyles.cardActions}>
                    {condition.conditionId === null && (
                      <button
                        type="button"
                        className={conditionStyles.linkButton}
                        onClick={() =>
                          setCustomForm({
                            mode: 'edit',
                            initial: {
                              name: condition.name,
                              code: condition.code ?? '',
                              description: condition.description ?? '',
                              symptoms: condition.suggestedSymptoms.map((s) => ({
                                id: s.id,
                                name: s.name,
                              })),
                            },
                          })
                        }
                      >
                        수정하기
                      </button>
                    )}
                    <button
                      type="button"
                      className={conditionStyles.removeButton}
                      onClick={() =>
                        updateConditions((prev) =>
                          prev.filter((c) => c.name !== condition.name),
                        )
                      }
                    >
                      삭제
                    </button>
                  </div>
                </div>

                <div className={styles.cardMeta}>
                  <div>질병 분류 코드: {condition.code || '-'}</div>
                  <div>내용: {condition.description || '-'}</div>
                </div>

                <div className={styles.cardSection}>
                  <p className={styles.cardSectionLabel}>주요 증상</p>
                  {condition.suggestedSymptoms.length > 0 ? (
                    <div className={styles.tagList}>
                      {condition.suggestedSymptoms.map((s) => (
                        <span key={s.id} className={styles.tag}>
                          {s.name}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className={styles.cardMeta}>없음</p>
                  )}
                </div>

                <div className={conditionStyles.statusRow}>
                  <Chip
                    label="진단 완료"
                    active={condition.status === 'DIAGNOSED'}
                    onClick={() =>
                      updateConditions((prev) =>
                        prev.map((c) =>
                          c.name === condition.name ? { ...c, status: 'DIAGNOSED' } : c,
                        ),
                      )
                    }
                  />
                  <Chip
                    label="진단 전"
                    active={condition.status === 'OBSERVING'}
                    onClick={() =>
                      updateConditions((prev) =>
                        prev.map((c) =>
                          c.name === condition.name ? { ...c, status: 'OBSERVING' } : c,
                        ),
                      )
                    }
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </ScreenBody>

      <ScreenFooter>
        <Button
          disabled={selected.length === 0}
          onClick={() => navigate(ROUTES.onboarding.conditionConfirm)}
        >
          다음
        </Button>
      </ScreenFooter>
    </Screen>
  )
}
