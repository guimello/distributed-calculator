/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tela;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

/**
 *
 * @author Edy
 */
public class FazCalculo implements ActionListener {

    private JTextField valor1;
    private JTextField valor2;
    private JTextArea  area;
    private JComboBox  operacao;

    public FazCalculo() {

    }

    public void actionPerformed(ActionEvent arg0)
    {
            // Aqui dentro vai o codigo dos Sockets - duas operações devem utilizar sockets tcps...e duas
            // operações devem utilizar sockets udp.
            // Devem ser implementados os servidores.

            String valorDoCampo1 = (getValor1().getText().trim().isEmpty()) ? "0" : getValor1().getText().trim();
            String valorDoCampo2 = (getValor2().getText().trim().isEmpty()) ? "0" : getValor2().getText().trim();

            String calculo = valorDoCampo1 + "#" + operacao.getSelectedIndex() + "#" + valorDoCampo2;

            //inner class abstrata que faz o parse dos números
            abstract class Parser
            {
                private int operando;
                private float operador1;
                private float operador2;

                public boolean parse(String query)
                {                    
                    StringTokenizer st = new StringTokenizer(query, "#");
                    operador1 = Float.parseFloat(st.nextToken());
                    operando = Integer.parseInt(st.nextToken());
                    operador2 = Float.parseFloat(st.nextToken());
                    return true;
                 }

                 public int getOperando()
                 {
                    return operando;
                  }

                 public float getOperador1()
                 {
                    return operador1;
                  }

                 public float getOperador2()
                 {
                    return operador2;
                  }

             }           

            //inner class abstrata que todo tipo de servidor (UDP, TCP) estende
            abstract class Server extends Parser implements Runnable, Math
            {
                private boolean ready;

                public boolean isReady()
                {
                    return ready;
                 }

                public void setReady(boolean b)
                {
                    ready = b;
                }
            }

            //inner class abstrata que todo tipo de cliente (UDP, TCP) estende
            abstract class Client implements Runnable
            {
                private String calculo;
                private String resposta = null;
                private Server server;

                public Client(String c, Server s)
                {
                    calculo = c;
                    server = s;
                 }

                public String getResposta()
                {
                   return resposta;
                 }

                public Server getServer()
                {
                    return server;
                }

                public String getCalculo()
                {
                    return calculo;
                }

                public void setResposta(String r)
                {
                    resposta = r;
                }
             }                

              

            String resposta = null;
            Server server;
            Client client;

            if(operacao.getSelectedIndex() < 2)
            {
                //anonymous inner class que estende a classe abstrata do servidor, aqui se torna um servidor TCP
                server = new Server()
                {
                    public void run( )
                    {
                        try
                        {
                            String query;
                            ServerSocket welcomeSocket = new ServerSocket(6789);

                            setReady(true);
                            synchronized(this)
                            {
                                notifyAll();
                             }

                            Socket connectionSocket = welcomeSocket.accept();

                            query =  new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())).readLine();
                            parse(query);

                            Float resultado = calcula(getOperando(), getOperador1(), getOperador2());

                            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                            outToClient.writeBytes(resultado.toString() + '\n');

                            welcomeSocket.close();
                        }
                        catch(Exception e){}
                    }

                    public float calcula(int operador, float operando1, float operando2)
                    {
                        switch(operador)
                        {
                            case 0: return operando1 + operando2;
                            case 1: return operando1 - operando2;
                            default: throw new IllegalArgumentException("operação inválida");
                        }
                    }
                 };

                //anonymous innder class que estende a classe abstrata do cliente, aqui se torna um clinete TCP
                client = new Client(calculo, server)
                {
                    public void run()
                    {
                        try
                        {
                            while(!getServer().isReady())
                            {
                                synchronized(getServer())
                                {
                                    getServer().wait();
                                }
                             }

                            Socket clientSocket = new Socket("localhost", 6789);
                            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                            outToServer.writeBytes(getCalculo() + '\n');
                            setResposta(inFromServer.readLine());
                            clientSocket.close();

                            synchronized(this)
                            {
                                notifyAll();
                             }
                         }
                         catch(Exception e){}
                    }
                };
             }
            else
            {
                //anonymous inner class que estende a classe abstrata do servidor, aqui se torna um servidor UDP
                server = new Server()
                {
                    public void run()
                    {
                        try
                        {
                            DatagramSocket serverSocket = new DatagramSocket(6789);
                            byte[] receiveData = new byte[1024];
                            byte[] sendData = new byte[1024];

                            System.out.println("1");
                            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                            System.out.println("2");
                            setReady(true);
                            synchronized(this)
                            {
                                notifyAll();
                             }

                            serverSocket.receive(receivePacket);
                            String query = new String(receivePacket.getData());

                            InetAddress IPAddress = receivePacket.getAddress();
                            int port = receivePacket.getPort();

                            parse(query);
                            Float resultado = calcula(getOperando(), getOperador1(), getOperador2());

                            sendData = resultado.toString().getBytes();
                            DatagramPacket sendPacket = new DatagramPacket (sendData,sendData.length, IPAddress, port);
                            serverSocket.send(sendPacket);
                            serverSocket.close();
                        }
                        catch(Exception e){System.out.println("s" +e.getMessage() + e.getClass());}
                    }

                    public float calcula(int operador, float operando1, float operando2)
                    {
                        switch(operador)
                        {
                            case 2: return operando1 * operando2;
                            case 3: return operando1 / operando2;
                            default: throw new IllegalArgumentException("operação inválida");
                        }
                    }
                };

                //anonymous inner class que estende a classe abstrata cliente, aqui se torna um cliente UDP
                client = new Client(calculo, server)
                {
                   public void run()
                    {
                        try
                        {
                            while(!getServer().isReady())
                            {
                                synchronized(getServer())
                                {
                                    getServer().wait();
                                }
                             }

                            DatagramSocket clientSocket = new DatagramSocket();
                            InetAddress IPAddress = InetAddress.getByName("localhost");

                            byte[] sendData = new byte[1024];
                            byte[] receiveData = new byte[1024];

                            sendData = getCalculo().getBytes();

                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,  6789);

                            clientSocket.send(sendPacket);

                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            clientSocket.receive(receivePacket);

                            setResposta(new String(receivePacket.getData()));
                            clientSocket.close();

                            synchronized(this)
                            {
                                notifyAll();
                             }
                        }
                        catch(Exception e){System.out.println(e.getMessage() + e.getClass());}
                    }
                };
             }

            //sobe os serviços
            Thread ts = new Thread(server);
            ts.start();
            Thread tc = new Thread(client);
            tc.start();

            while((resposta = client.getResposta()) == null)
            {
                synchronized(client)
                {
                    try
                    {
                        client.wait();
                     }
                    catch(InterruptedException e){}
                 }
             }

            System.out.println("Resultado da Operação: " + resposta);
            area.setText(area.getText().trim() + ((area.getText().trim().length() > 0)?"\n":"") + "Resultado da Operação: " + resposta + "\n ");
    }

    public JTextField getValor1() {
        return valor1;
    }

    public void setValor1(JTextField valor1) {
        this.valor1 = valor1;
    }

    public JTextField getValor2() {
        return valor2;
    }

    public void setValor2(JTextField valor2) {
        this.valor2 = valor2;
    }

    public JTextArea getArea() {
        return area;
    }

    public void setArea(JTextArea area) {
        this.area = area;
    }

    /**
     * @return the operacao
     */
    public JComboBox getOperacao() {
        return operacao;
    }

    /**
     * @param operacao the operacao to set
     */
    public void setOperacao(JComboBox operacao) {
        this.operacao = operacao;
    }

}

//interface que faz o contrato calcula
interface Math
{
    public float calcula(int operador, float operando1, float operando2) throws IllegalArgumentException;
}
