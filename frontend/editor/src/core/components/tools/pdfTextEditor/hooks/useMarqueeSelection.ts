import { RefObject, useEffect, useState } from "react";
import { TextGroup } from "@app/tools/pdfTextEditor/pdfTextEditorTypes";

export interface MarqueeBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface UseMarqueeSelectionProps {
  containerRef: RefObject<HTMLDivElement | null>;
  pageGroups: TextGroup[];
  scale: number;
  onSelectionComplete: (selectedIds: string[], append: boolean) => void;
  disabled?: boolean;
}

export function useMarqueeSelection({
  containerRef,
  pageGroups,
  scale,
  onSelectionComplete,
  disabled = false,
}: UseMarqueeSelectionProps) {
  const [marqueeBox, setMarqueeBox] = useState<MarqueeBox | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container || disabled) return;

    let isDragging = false;
    let startX = 0;
    let startY = 0;
    let hasMoved = false;

    const handleMouseDown = (e: MouseEvent) => {
      // Only trigger on direct clicks to the container
      if (e.target !== container) return;

      isDragging = true;
      hasMoved = false;
      const rect = container.getBoundingClientRect();
      startX = e.clientX - rect.left;
      startY = e.clientY - rect.top;

      setMarqueeBox({ x: startX, y: startY, width: 0, height: 0 });
    };

    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging) return;

      const rect = container.getBoundingClientRect();
      const currentX = Math.max(0, Math.min(e.clientX - rect.left, rect.width));
      const currentY = Math.max(0, Math.min(e.clientY - rect.top, rect.height));

      if (Math.abs(currentX - startX) > 5 || Math.abs(currentY - startY) > 5) {
        hasMoved = true;
      }

      setMarqueeBox({
        x: Math.min(startX, currentX),
        y: Math.min(startY, currentY),
        width: Math.abs(currentX - startX),
        height: Math.abs(currentY - startY),
      });
    };

    const handleMouseUp = (e: MouseEvent) => {
      if (!isDragging) return;
      isDragging = false;

      if (!hasMoved) {
        setMarqueeBox(null);
        return;
      }

      const rect = container.getBoundingClientRect();
      const currentX = Math.max(0, Math.min(e.clientX - rect.left, rect.width));
      const currentY = Math.max(0, Math.min(e.clientY - rect.top, rect.height));

      const finalBox = {
        x: Math.min(startX, currentX),
        y: Math.min(startY, currentY),
        width: Math.abs(currentX - startX),
        height: Math.abs(currentY - startY),
      };

      const pdfBox = {
        left: finalBox.x / scale,
        right: (finalBox.x + finalBox.width) / scale,
        top: finalBox.y / scale,
        bottom: (finalBox.y + finalBox.height) / scale,
      };

      const selectedIds = pageGroups
        .filter((g) => {
          return !(
            g.bounds.right < pdfBox.left ||
            g.bounds.left > pdfBox.right ||
            g.bounds.bottom < pdfBox.top ||
            g.bounds.top > pdfBox.bottom
          );
        })
        .map((g) => g.id);

      onSelectionComplete(selectedIds, e.shiftKey || e.ctrlKey || e.metaKey);
      setMarqueeBox(null);
    };

    container.addEventListener("mousedown", handleMouseDown);
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);

    return () => {
      container.removeEventListener("mousedown", handleMouseDown);
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [containerRef, pageGroups, scale, onSelectionComplete, disabled]);

  return marqueeBox;
}
