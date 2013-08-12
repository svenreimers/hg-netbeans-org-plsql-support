/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.fold;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.netbeans.api.editor.fold.FoldType;

/**
 *
 * @author ChrLSE
 */
public class FoldSearchObjectTest {

   @Test
   public void shouldBeEqualWithAndWithoutBackingFoldObject() {
      int startOffset = 100;
      int endOffset = 200;
      FoldType foldType = PlsqlFoldTypes.COMMENT;
      FoldSearchObject searchObject1 = new FoldSearchObject(startOffset, endOffset, foldType);
      FoldAdapter fold = mock(FoldAdapter.class);
      FoldSearchObject searchObject2 = new FoldSearchObject(fold);
      when(fold.getStartOffset()).thenReturn(startOffset);
      when(fold.getEndOffset()).thenReturn(endOffset);
      when(fold.getFoldType()).thenReturn(foldType);
      assertEquals(searchObject1, searchObject2);
   }
}