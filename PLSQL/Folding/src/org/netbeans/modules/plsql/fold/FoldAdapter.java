/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.fold;

import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldType;

/**
 *
 * @author ChrLSE
 */
class FoldAdapter {

   private final Fold fold;

   FoldAdapter(Fold fold) {
      this.fold = fold;
   }

   int getStartOffset() {
      return fold.getStartOffset();
   }

   int getEndOffset() {
      return fold.getEndOffset();
   }
   
   FoldType getFoldType(){
        return fold.getType();   
   }
}
