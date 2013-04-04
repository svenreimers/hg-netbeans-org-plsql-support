/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.format;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.plsql.filetype.PlsqlDataLoader;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.openide.util.Lookup;

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
      PlsqlBlockFactory blockFactory = getBlockFactory((BaseDocument) context.getDocument());
      if (blockFactory.isBlockAtOffsetOfType(context.getCaretOffset(), PlsqlBlockType.COMMENT)) {
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

   private PlsqlBlockFactory getBlockFactory(BaseDocument doc) {
      final Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof Lookup.Provider) {
         return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
      }
      return null;
   }

   @MimeRegistration(mimeType = PlsqlDataLoader.REQUIRED_MIME, service = TypedBreakInterceptor.Factory.class)
   public static class PlsqlFactory implements TypedBreakInterceptor.Factory {

      @Override
      public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
         return new PlsqlTypedBreakInterceptor();
      }
   }
}
