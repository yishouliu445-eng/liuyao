/** 单爻信息 — 驱动 HexagramLine CSS 绘制 */
export interface LineInfoDTO {
  index: number;
  yinYang: string;       // "阴" | "阳"
  moving: boolean;        // 动爻标记
  changeTo: string;       // 变爻去向
  liuQin: string;         // 六亲
  liuShen: string;        // 六神
  branch: string;         // 地支
  wuXing: string;         // 五行
  changeBranch: string;   // 变爻地支
  changeWuXing: string;   // 变爻五行
  changeLiuQin: string;   // 变爻六亲
  shi: boolean;           // 世爻
  ying: boolean;          // 应爻
}

/** 神煞命中 */
export interface ShenShaHitDTO {
  code: string;
  name: string;
  scope: string;
  branch: string;
  matchedBy: string;
  summary: string;
  lineIndexes: number[];
  evidence: Record<string, unknown>;
}

/** 卦象快照 */
export interface ChartSnapshotDTO {
  question: string;
  questionCategory: string;
  divinationMethod: string;
  divinationTime: string;
  mainHexagram: string;
  changedHexagram: string;
  mainHexagramCode: string;
  changedHexagramCode: string;
  mainUpperTrigram: string;
  mainLowerTrigram: string;
  changedUpperTrigram: string;
  changedLowerTrigram: string;
  mutualHexagram: string;
  mutualHexagramCode: string;
  oppositeHexagram: string;
  oppositeHexagramCode: string;
  reversedHexagram: string;
  reversedHexagramCode: string;
  palace: string;
  palaceWuXing: string;
  shi: number;
  ying: number;
  useGod: string;
  riChen: string;
  yueJian: string;
  snapshotVersion: string;
  calendarVersion: string;
  kongWang: string[];
  shenShaHits: ShenShaHitDTO[];
  lines: LineInfoDTO[];
}
