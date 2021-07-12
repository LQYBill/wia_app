export class CandidatePool {
  constructor(candidates, accessKey) {
    this.all = new Map()
    this.selected = new Set()
    candidates.forEach(
      c => {
        this.all.set(accessKey(c), c)
      }
    )
    console.log("constructed all of element:", this.all)
  }

  add(key) {
    this.addAll([key])
  }

  addAll(keys) {
    keys.forEach(key => this.selected.add(key))
  }

  remove(key) {
    this.removeAll([key])
  }

  removeAll(keys) {
    keys.forEach(key => this.selected.delete(key))
  }


  getCandidates() {
    let res = new Set()
    for (const [key, value] of this.all.entries()) {
      if (!this.selected.has(key)) {
        res.add(value)
      }
    }
    console.log("candidates:", res)
    return res;
  }

  getPool() {
    let res = new Set()
    for (const [key, value] of this.all.entries()) {
      if (this.selected.has(key)) {
        res.add(value)
      }
    }
    console.log("pool:", res)
    return res;
  }


}