import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'

import { Button } from '@/components/ui/Button'
import { TextField } from '@/components/ui/TextField'
import { searchSymptoms } from '@/features/onboarding/api'
import type { PickedSymptom } from '@/features/onboarding/types'

import styles from './onboarding.module.css'
import formStyles from './CustomConditionForm.module.css'

export type CustomConditionDraft = {
  name: string
  code: string
  description: string
  symptoms: PickedSymptom[]
}

type CustomConditionFormProps = {
  /** 처음 등록인지 이미 등록한 항목의 수정인지 — 문구와 버튼이 달라진다. */
  mode: 'create' | 'edit'
  initial: CustomConditionDraft
  onSubmit: (draft: CustomConditionDraft) => void
  onCancel: () => void
}

/**
 * 마스터에 없는 질환을 직접 등록하는 폼.
 *
 * 코드·설명·증상은 모두 선택이다. "잘 모르겠어요"로 이름만 저장할 수 있어야 한다 —
 * 진단명을 정확히 아는 사용자만 있는 게 아니다.
 */
export function CustomConditionForm({
  mode,
  initial,
  onSubmit,
  onCancel,
}: CustomConditionFormProps) {
  const [code, setCode] = useState(initial.code)
  const [description, setDescription] = useState(initial.description)
  const [symptoms, setSymptoms] = useState<PickedSymptom[]>(initial.symptoms)
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState('')

  const trimmedQuery = query.trim()
  const { data: results = [] } = useQuery({
    queryKey: ['symptoms', trimmedQuery],
    queryFn: () => searchSymptoms(trimmedQuery),
    enabled: trimmedQuery.length > 0,
  })

  const notPicked = results.filter((r) => !symptoms.some((s) => s.id === r.id))

  const addSymptom = (id: number, name: string) => {
    setSymptoms([...symptoms, { id, name }])
    setQuery('')
  }

  const submit = (withDetails: boolean) =>
    onSubmit({
      name: initial.name,
      code: withDetails ? code.trim() : '',
      description: withDetails ? description.trim() : '',
      symptoms: withDetails ? symptoms : [],
    })

  return (
    <div className={formStyles.form}>
      <p className={formStyles.formTitle}>
        &lsquo;{initial.name}&rsquo; {mode === 'edit' ? '정보 수정' : '직접 등록'}
      </p>

      <div>
        <label className={styles.fieldLabel} htmlFor="custom-code">
          분류 코드 (선택)
        </label>
        <TextField
          id="custom-code"
          placeholder="예: K50"
          value={code}
          onChange={(e) => setCode(e.target.value)}
        />
      </div>

      <div>
        <label className={styles.fieldLabel} htmlFor="custom-desc">
          질병 설명 (선택)
        </label>
        <textarea
          id="custom-desc"
          className={formStyles.textarea}
          placeholder="어떤 질환인지 간단히 적어주세요"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      <div>
        <span className={styles.fieldLabel}>주요 증상 (선택)</span>

        {symptoms.length > 0 && (
          <div className={formStyles.symptomTags}>
            {symptoms.map((s, index) => (
              <button
                key={`${s.id ?? 'custom'}-${s.name}`}
                type="button"
                className={formStyles.symptomTag}
                onClick={() => setSymptoms(symptoms.filter((_, i) => i !== index))}
              >
                {s.name} ✕
              </button>
            ))}
          </div>
        )}

        {searchOpen ? (
          <>
            <TextField
              autoFocus
              placeholder="증상을 검색해보세요 (예: 두통)"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            {trimmedQuery && (
              <div className={formStyles.symptomResults}>
                {notPicked.map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    className={formStyles.symptomResultItem}
                    onClick={() => addSymptom(s.id, s.name)}
                  >
                    {s.name}
                    {/* 일상 표현으로 찾은 경우 어떤 말로 걸렸는지 보여준다. */}
                    {s.nameKo && <span className={formStyles.symptomResultMeta}>{s.nameKo}</span>}
                  </button>
                ))}
                {notPicked.length === 0 && (
                  <p className={formStyles.symptomEmpty}>일치하는 증상이 없어요</p>
                )}
              </div>
            )}
          </>
        ) : (
          <p className={formStyles.symptomStatus}>
            {symptoms.length > 0 ? '증상을 등록했어요' : '증상을 등록하지 않았어요'}
          </p>
        )}

        <div className={formStyles.symptomActions}>
          <button
            type="button"
            className={formStyles.textButton}
            onClick={() => setSearchOpen(true)}
          >
            + 증상 추가하기
          </button>
          {searchOpen && (
            <button
              type="button"
              className={formStyles.textButtonMuted}
              onClick={() => {
                setSearchOpen(false)
                setQuery('')
              }}
            >
              증상 등록 안 하기
            </button>
          )}
        </div>
      </div>

      <div className={formStyles.formActions}>
        <Button variant="outline" size="md" onClick={onCancel}>
          취소
        </Button>
        <Button size="md" onClick={() => submit(true)}>
          {mode === 'edit' ? '저장' : '추가'}
        </Button>
      </div>

      {mode === 'create' && (
        <button type="button" className={formStyles.unknownButton} onClick={() => submit(false)}>
          잘 모르겠어요 · 그냥 저장
        </button>
      )}
    </div>
  )
}
