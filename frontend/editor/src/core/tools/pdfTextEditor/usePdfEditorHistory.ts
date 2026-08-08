import { useCallback, useState } from "react";
import {
  TextGroup,
  PdfJsonImageElement,
} from "@app/tools/pdfTextEditor/pdfTextEditorTypes";

export interface EditorHistoryState {
  groupsByPage: TextGroup[][];
  imagesByPage: PdfJsonImageElement[][];
}

export function usePdfEditorHistory(
  initialState: EditorHistoryState,
  onChange: (state: EditorHistoryState) => void,
) {
  const [history, setHistory] = useState<EditorHistoryState[]>([]);
  const [currentIndex, setCurrentIndex] = useState(-1);

  const pushState = useCallback(
    (newState: EditorHistoryState) => {
      setHistory((prev) => {
        const newHistory = prev.slice(0, currentIndex + 1);
        newHistory.push(newState);
        // Keep max 50 states to prevent memory leaks
        if (newHistory.length > 50) {
          newHistory.shift();
        }
        return newHistory;
      });
      setCurrentIndex((prev) => Math.min(prev + 1, 49)); // Max index is 49 if max length is 50
    },
    [currentIndex],
  );

  const undo = useCallback(() => {
    if (currentIndex > 0) {
      const prevIndex = currentIndex - 1;
      setCurrentIndex(prevIndex);
      onChange(history[prevIndex]);
    } else if (currentIndex === 0) {
      // Revert to initial state (before first action)
      setCurrentIndex(-1);
      onChange(initialState);
    }
  }, [currentIndex, history, onChange, initialState]);

  const redo = useCallback(() => {
    if (currentIndex < history.length - 1) {
      const nextIndex = currentIndex + 1;
      setCurrentIndex(nextIndex);
      onChange(history[nextIndex]);
    }
  }, [currentIndex, history, onChange]);

  const resetHistory = useCallback(() => {
    setHistory([]);
    setCurrentIndex(-1);
  }, []);

  return {
    pushState,
    undo,
    redo,
    canUndo: currentIndex >= 0,
    canRedo: currentIndex < history.length - 1,
    resetHistory,
  };
}
