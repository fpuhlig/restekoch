export interface Recipe {
  id: string
  title: string
  ingredients: string[]
  directions: string[]
  ner: string[]
}

export interface ScanResponse {
  ingredients: string[]
  recipes: Recipe[]
  explanation: string
  cached: boolean
}
