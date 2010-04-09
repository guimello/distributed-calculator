/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tela;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.swing.*;


/**
 *
 * @author Edy
 */
public class EnviaParaTodos implements  ActionListener {

    private  JTextArea resultado;

    public EnviaParaTodos(JTextArea j)
    {
        resultado = j;
    }

    public void actionPerformed(ActionEvent event)
    {

        //Aqui será implementada a ação quando o usuário clicar no botão enviar para todos.
        // Deve ser criado um mensagem que deverá ser enviada para um grupo multicast.

        try
        {
            //anonymous inner class que implementa a interface Runnable, serão os ouvintes do multicast
            Runnable ouvinte = new Runnable()
            {
                public void run()
                {
                   try
                   {
                        MulticastSocket mcs = new MulticastSocket(55000);
                        InetAddress grp = InetAddress.getByName("239.0.0.1");
                        mcs.joinGroup(grp);

                        byte rec[] = new byte[256];
                        DatagramPacket pkg = new DatagramPacket(rec, rec.length);                        
                        mcs.receive(pkg);

                        System.out.println("Dados recebidos:" + new String(pkg.getData()));
                   }
                   catch(IOException e){}
                }
            };

            //vamos iniciar vários ouvintes
            for(int i = 0; i < 5; i++)
                new Thread(ouvinte).start();

            String [] linhas = resultado.getText().split("\n");
            String mensagem = "";           

            for(int i = linhas.length - 1; i >= 0; i--)
            {
                mensagem = linhas[i].trim();

                if(!mensagem.isEmpty()) break;
                
                if(i == 0)
                    mensagem = "Não há nada a ser enviado";
            }

            System.out.println("mensagem => " + mensagem);
            JOptionPane.showMessageDialog(((JButton)event.getSource()).getParent(), mensagem);

            byte[] b = mensagem.getBytes();
            InetAddress addr = InetAddress.getByName("239.0.0.1");
            DatagramSocket ds = new DatagramSocket();
            DatagramPacket pkg = new DatagramPacket(b, b.length, addr, 55000);

            ds.send(pkg); //envia o pacote de mensagem
            ds.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();

            System.out.println("Nao foi possivel enviar a mensagem");
        }
    }

}
