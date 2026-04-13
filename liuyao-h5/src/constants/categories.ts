export interface CategoryOption {
  value: string;
  label: string;
}

export const QUESTION_CATEGORIES: CategoryOption[] = [
  { value: '感情', label: '感情' },
  { value: '婚姻', label: '婚姻' },
  { value: '工作', label: '工作' },
  { value: '事业', label: '事业' },
  { value: '收入', label: '收入' },
  { value: '财运', label: '财运' },
  { value: '投资', label: '投资' },
  { value: '考试', label: '考试' },
  { value: '出行', label: '出行' },
  { value: '健康', label: '健康' },
  { value: '合作', label: '合作' },
  { value: '官司', label: '官司' },
  { value: '房产', label: '房产' },
  { value: '搬家', label: '搬家' },
  { value: '寻人', label: '寻人' },
  { value: '寻物', label: '寻物' },
  { value: '失物', label: '失物' },
  { value: '学业', label: '学业' },
  { value: '交友', label: '交友' },
  { value: '其他', label: '其他' },
];

/** 规则分类英文 → 中文映射 */
export const RULE_CATEGORY_MAP: Record<string, string> = {
  YONGSHEN_STATE: '用神状态',
  SHI_STATE: '世爻状态',
  SHI_YING: '世应联系',
  MOVING_CHANGE: '动变影响',
  COMPOSITE: '综合裁定',
};
