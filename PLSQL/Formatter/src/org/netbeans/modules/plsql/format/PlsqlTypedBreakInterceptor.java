package org.netbeans.modules.plsql.format;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.plsql.filetype.PlsqlDataLoader;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;

/**
 *
 * @author ChrLSE
 */
public class PlsqlTypedBreakInterceptor implements TypedBreakInterceptor {

   private static final Logger LOG = Logger.getLogger(PlsqlTypedBreakInterceptor.class.getName());

   @Override
   public boolean beforeInsert(Context context) throws BadLocationException {
      LOG.log(Level.FINER, "beforeInsert, context: {0}", context);
      return false;
   }

   @Override
   public void insert(MutableContext context) throws BadLocationException {
      LOG.log(Level.FINER, "insert, context: {0}", context);
      if (!(context.getDocument() instanceof BaseDocument)) {
         return;
      }
      BaseDocument doc = (BaseDocument) context.getDocument();
      int insertPos = context.getCaretOffset();
      if (wordInRowBelowStartsWith(doc, insertPos, "--")) {
         context.setText("\n--  ", 0, 5);
      }
   }

   @Override
   public void afterInsert(Context context) throws BadLocationException {
      LOG.log(Level.FINER, "afterInsert, context: {0}", context);
   }

   @Override
   public void cancelled(Context context) {
      LOG.log(Level.FINER, "cancelled, context: {0}", context);
   }

   private boolean wordInRowBelowStartsWith(BaseDocument doc, int insertPos, String word) throws BadLocationException {
      int currentRow = Utilities.getLineOffset(doc, insertPos);
      int rowStartFromLineOffset = Utilities.getRowStartFromLineOffset(doc, currentRow + 1);
      return Utilities.getWord(doc, rowStartFromLineOffset).startsWith(word);
   }

   @MimeRegistration(mimeType = PlsqlDataLoader.REQUIRED_MIME, service = TypedBreakInterceptor.Factory.class)
   public static class PlsqlFactory implements TypedBreakInterceptor.Factory {

      @Override
      public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
         return new PlsqlTypedBreakInterceptor();
      }
   }
}
