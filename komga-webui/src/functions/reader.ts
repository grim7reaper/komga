import { ContinuousScaleType, PagedReaderLayout, ScaleType } from '@/types/enum-reader'
import { ReadingDirection } from '@/types/enum-books'

export const ScaleTypeText = {
  [ScaleType.SCREEN]: 'Fit screen',
  [ScaleType.HEIGHT]: 'Fit height',
  [ScaleType.WIDTH]: 'Fit width',
  [ContinuousScaleType.WIDTH]: 'Fit width',
  [ScaleType.ORIGINAL]: 'Original',
  [ContinuousScaleType.ORIGINAL]: 'Original',
}

export const ReadingDirectionText = {
  [ReadingDirection.LEFT_TO_RIGHT]: 'Left to right',
  [ReadingDirection.RIGHT_TO_LEFT]: 'Right to left',
  [ReadingDirection.VERTICAL]: 'Vertical',
  [ReadingDirection.WEBTOON]: 'Webtoon',
}

export const PagedReaderLayoutText = {
  [PagedReaderLayout.SINGLE_PAGE]: 'Single page',
  [PagedReaderLayout.DOUBLE_PAGES]: 'Double pages',
  [PagedReaderLayout.DOUBLE_NO_COVER]: 'Double pages (no cover)',
}
