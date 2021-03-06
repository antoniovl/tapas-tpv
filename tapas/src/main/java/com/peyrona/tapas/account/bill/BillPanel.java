/*
 * Copyright (C) 2010 Francisco José Morero Peyrona. All Rights Reserved.
 *
 * This file is part of Tapas project: http://code.google.com/p/tapas-tpv/
 *
 * GNU Classpath is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the free
 * Software Foundation; either version 3, or (at your option) any later version.
 *
 * Tapas is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Tapas; see the file COPYING.  If not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.peyrona.tapas.account.bill;

import com.peyrona.tapas.Utils;
import com.peyrona.tapas.account.BillAndMenuPanel;
import com.peyrona.tapas.persistence.Product;
import com.peyrona.tapas.persistence.Bill;
import com.peyrona.tapas.swing.ImageHighlightFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.FilteredImageSource;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Cuando se abre el detalle de una cuenta, aparecen dos paneles: este es el de
 * la izquierda, que contiene el nombre del cliente, lo que ha consumido y los
 * botones para operar con ello y el de la derecha donde aparecen todos los
 * artículos (bebidas y comidas) pre-establecidas.
 *
 * @author Francisco Morero Peyrona
 */
public final class BillPanel extends JPanel
{
    private Bill bill;

    // GUI -------------------------------
    private BillOwnerPanel    pnlCustomer;
    private PaymentPanel      pnlPayMode;
    private ItemsPanel        pnlItems;
    private ItemEditorPanel   pnlEditItems;
    private NumericPadPanel   pnlNumericPad;
    private TotalDisplayPanel pnlDisplay;

    //------------------------------------------------------------------------//

    public BillPanel( Bill bill )
    {
        assert bill != null : "Bill no puede ser null";

        this.bill = bill;

        initComponents();
        updateButtons();
    }

    //----------------------------------------------------------------------------//

    public Bill getBill()
    {
        bill.setCustomer( pnlCustomer.getCustomerName() );
        bill.setPayment( pnlPayMode.getPayMode() );
        bill.setLines( pnlItems.getBillLines() );

        return bill;
    }

    public String getCustomerName()
    {
        return pnlCustomer.getCustomerName();
    }

    public void addProduct( Product product )
    {
        pnlItems.add( product );
        updateButtons();
    }

    //------------------------------------------------------------------------//

    private void updateButtons()
    {
        pnlPayMode.setEnabled( pnlItems.getTotal().doubleValue() > 0 );
        pnlEditItems.setEnabled( pnlItems.isRowSelected() );
        pnlDisplay.setAmount( pnlItems.getTotal() );
    }

    private void initComponents()
    {
        pnlCustomer   = new BillOwnerPanel( bill.getCustomer() );
        pnlPayMode    = new PaymentPanel();
        pnlItems      = new ItemsPanel( bill.getLines() );
        pnlNumericPad = new NumericPadPanel();
        pnlEditItems  = new ItemEditorPanel();
        pnlDisplay    = new TotalDisplayPanel();

        pnlPayMode.setEnabled( pnlItems.getTotal().doubleValue() > 0 );
        pnlNumericPad.setVisible( false );
        pnlEditItems.setEnabled( pnlItems.isRowSelected() );

        JPanel pnlButtonsAndDisplay = new JPanel( new BorderLayout( 30, 0 ) );
               pnlButtonsAndDisplay.setBorder( new EmptyBorder( 20, 0, 10, 0 ) );
               pnlButtonsAndDisplay.add( pnlEditItems, BorderLayout.WEST   );
               pnlButtonsAndDisplay.add( pnlDisplay  , BorderLayout.CENTER );

        JPanel pnlOwnerAndPayments = new JPanel( new BorderLayout() );
               pnlOwnerAndPayments.add( pnlCustomer, BorderLayout.NORTH  );
               pnlOwnerAndPayments.add( pnlPayMode , BorderLayout.CENTER );

        JPanel pnlItemsAndNumericPad = new JPanel( new BorderLayout( 0, 7 ) );
               pnlItemsAndNumericPad.add( pnlItems     , BorderLayout.CENTER );
               pnlItemsAndNumericPad.add( pnlNumericPad, BorderLayout.SOUTH  );

        setLayout( new BorderLayout() );
               setBorder( new CompoundBorder( new LineBorder(Color.gray, 2, true ),
                                              new EmptyBorder( 7,7,7,7 ) ) );

        setMinimumSize( BillAndMenuPanel.SUBPANEL_DIMENSION );
        setPreferredSize( getMinimumSize() );
        add( pnlOwnerAndPayments  , BorderLayout.NORTH  );
        add( pnlItemsAndNumericPad, BorderLayout.CENTER );
        add( pnlButtonsAndDisplay , BorderLayout.SOUTH  );
    }

    //------------------------------------------------------------------------//
    // Inner Class: Panel con los botones de edición de la línea resaltada del ticket.
    //------------------------------------------------------------------------//
    private final class ItemEditorPanel extends JPanel
    {
        ItemEditorPanel()
        {
            super( new GridLayout( 1, 4, 4, 0 ) );

            add( new Button4Item( Button4Item.CMD_DEL  , "Elimina la línea resaltada" ) );
            add( new Button4Item( Button4Item.CMD_EDIT , "Edita el precio de la línea resaltada" ) );
            add( new Button4Item( Button4Item.CMD_MINUS, "Decrementa las unidades de la línea resaltada" ) );
            add( new Button4Item( Button4Item.CMD_PLUS , "Incrementa las unidades de la línea resaltada" ) );
        }

        @Override
        public void setEnabled( boolean b )
        {
            super.setEnabled( b );

            for( Component c : getComponents() )
            {
                c.setEnabled( b );
            }
        }
    }

    //------------------------------------------------------------------------//
    // Inner Class: Editor para cambiar el precio de la línea resaltada del ticket.
    //------------------------------------------------------------------------//
    private final class ItemPriceEditor implements ActionListener
    {
        private StringBuilder sb = new StringBuilder();

        private void startEditing()
        {
            if( BillPanel.this.pnlItems.isRowSelected() )
            {
                BillPanel.this.pnlPayMode.setEnabled( false );
                BillPanel.this.pnlNumericPad.setVisible( true );
                BillPanel.this.pnlEditItems.setEnabled( false );
                BillPanel.this.pnlItems.startEditingPrice();
                BillPanel.this.pnlItems.updateEditingPrice( "0" );
                BillPanel.this.pnlNumericPad.addActionListener( this );
            }
        }

        private void stopEditing()
        {
            BillPanel.this.pnlPayMode.setEnabled( true );
            BillPanel.this.pnlNumericPad.setVisible( false );
            BillPanel.this.pnlEditItems.setEnabled( true );
            BillPanel.this.pnlItems.stopEditingPrice();
            BillPanel.this.pnlDisplay.setAmount( pnlItems.getTotal() );
            BillPanel.this.pnlNumericPad.removeActionListener( this );
        }

        @Override
        public void actionPerformed( ActionEvent ae )
        {
            char cBtn = ae.getActionCommand().charAt( 0 );

            if( cBtn == NumericPadPanel.cENTER )
            {
                stopEditing();
            }
            else
            {
                if( cBtn == Utils.cDecimalSep && notExist() ) sb.append( cBtn );
                else if( cBtn == NumericPadPanel.cCLEAR )     sb.setLength( 0 );
                else                                          sb.append( cBtn );

                BillPanel.this.pnlItems.updateEditingPrice( sb.toString() );
            }
        }

        private boolean notExist()     // Comprueba que no existe el DecimalSeparator en sb
        {
            return (sb.indexOf( String.valueOf( Utils.cDecimalSep ) ) == -1);
        }
    }

    //------------------------------------------------------------------------//
    // Inner Class: Los botones para editar las líneas del ticket: incrementar y
    //              decrementar las unidades, cambiar el precio o borrarla.
    //------------------------------------------------------------------------//
    private final class Button4Item extends JButton implements ActionListener
    {
        final static String CMD_MINUS = "item_minus";
        final static String CMD_PLUS  = "item_plus";
        final static String CMD_EDIT  = "item_edit";
        final static String CMD_DEL   = "item_del";

        Button4Item( String sActionCommand, String sToolTip )
        {
            // Con este filtro fabrico el icono de disabled y me ahorro la mitad de los iconos
            ImageHighlightFilter ihf = new ImageHighlightFilter( true, 64 );

            ImageIcon icon  = new ImageIcon( getClass().getResource( "/com/peyrona/tapas/account/bill/images/"+ sActionCommand +".png" ) );
            Image     image = createImage( new FilteredImageSource( icon.getImage().getSource(), ihf ) );
            ImageIcon dicon = new ImageIcon( image );

            setActionCommand( sActionCommand );
            setToolTipText( sToolTip );
            setIcon( icon );
            setDisabledIcon( dicon );
            setMargin( new Insets( 4, 0, 0, 4 ) );
            setFocusPainted( false );
            addActionListener( Button4Item.this );
        }

        @Override
        public void actionPerformed( ActionEvent e )
        {
            String sCmd = getActionCommand();

                 if( sCmd.equals( CMD_MINUS ) )  BillPanel.this.pnlItems.decrementQuantity();
            else if( sCmd.equals( CMD_PLUS  ) )  BillPanel.this.pnlItems.incrementQuantity();
            else if( sCmd.equals( CMD_EDIT  ) )  (new ItemPriceEditor()).startEditing();
            else if( sCmd.equals( CMD_DEL   ) )  BillPanel.this.pnlItems.deleteLine();

            BillPanel.this.updateButtons();
        }
    }
}