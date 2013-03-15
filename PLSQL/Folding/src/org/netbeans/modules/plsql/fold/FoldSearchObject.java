/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.fold;

import java.io.Serializable;

/**
 *
 * @author ChrLSE
 */
class FoldSearchObject implements Serializable {

   private static final long serialVersionUID = 1L;
   private final int startOffset;
   private final int endOffset;
   private FoldAdapter fold;

   FoldSearchObject(int startOffset, int endOffset) {
      this.startOffset = startOffset;
      this.endOffset = endOffset;
   }

   FoldSearchObject(FoldAdapter fold) {
      this(-1, -1);
      this.fold = fold;
   }

   public int getStartOffset() {
      if (startOffset == -1) {
         return fold.getStartOffset();
      }
      return startOffset;
   }

   public int getEndOffset() {
      if (endOffset == -1) {
         return fold.getEndOffset();
      }
      return endOffset;
   }

   @Override
   public int hashCode() {
      int hash = 7;
      hash = 13 * hash + this.getStartOffset();
      hash = 13 * hash + this.getEndOffset();
      return hash;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      final FoldSearchObject other = (FoldSearchObject) obj;
      if (this.getStartOffset() != other.getStartOffset()) {
         return false;
      }
      if (this.getEndOffset() != other.getEndOffset()) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      return "FoldSearchObject{" + "startOffset=" + getStartOffset() + ", endOffset=" + getEndOffset() + '}';
   }
}
