export class PracticeElapsedTimer {
  private readonly elapsed = new Map<number, number>()
  private activeQuestionId: number | null = null
  private activeSince = 0

  constructor(private readonly now: () => number = () => Date.now()) {}

  enter(questionId: number) {
    if (this.activeQuestionId === questionId) return
    this.pause()
    this.activeQuestionId = questionId
    this.activeSince = this.now()
  }

  pause() {
    if (this.activeQuestionId === null) return
    const passed = Math.max(0, Math.floor((this.now() - this.activeSince) / 1000))
    this.elapsed.set(this.activeQuestionId, (this.elapsed.get(this.activeQuestionId) ?? 0) + passed)
    this.activeQuestionId = null
    this.activeSince = 0
  }

  seconds(questionId: number) {
    const stored = this.elapsed.get(questionId) ?? 0
    if (this.activeQuestionId !== questionId) return stored
    return stored + Math.max(0, Math.floor((this.now() - this.activeSince) / 1000))
  }
}
