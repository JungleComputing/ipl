/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisException;
import ibis.ipl.PortType;
import ibis.ipl.StaticProperties;

import java.io.IOException;
import java.util.Vector;

public abstract class Initialization extends SatinBase {

    /**
     * This method parses the commandline arguments. All flags destined for
     * Satin start with "satin-". The flags that are recognized are: <table>
     * <tr>
     * <td>-satin-closed</td>
     * <td>all members join the run during startup.</td>
     * </tr>
     * <tr>
     * <td>-satin-panda</td>
     * <td>use the Panda version of Ibis.</td>
     * </tr>
     * <tr>
     * <td>-satin-mpi</td>
     * <td>use the MPI version of Ibis.</td>
     * </tr>
     * <tr>
     * <td>-satin-net</td>
     * <td>use NetIBis</td>
     * </tr>
     * <tr>
     * <td>-satin-nio</td>
     * <td>use NIO Ibis</td>
     * </tr>
     * <tr>
     * <td>-satin-tcp</td>
     * <td>use TCP Ibis</td>
     * </tr>
     * <tr>
     * <td>-satin-stats</td>
     * <td>display statistics at the end of the run</td>
     * </tr>
     * <tr>
     * <td>-satin-no-stats</td>
     * <td>don't display statistics at the end of the run</td>
     * </tr>
     * <tr>
     * <td>-satin-detailed-stats</td>
     * <td>display detailed statistics for every member at the end of the run
     * </td>
     * </tr>
     * <tr>
     * <td>-satin-ibis</td>
     * <td>use Ibis serialization</td>
     * </tr>
     * <tr>
     * <td>-satin-sun</td>
     * <td>use Sun serialization</td>
     * </tr>
     * <tr>
     * <td>-satin-no-upcalls</td>
     * <td>use explicit receive for receiving messages</td>
     * </tr>
     * <tr>
     * <td>-satin-upcalls</td>
     * <td>use upcalls for receiving messages</td>
     * </tr>
     * <tr>
     * <td>-satin-upcall-polling</td>
     * <td>use upcalls for receiving messages, explicit poll is required to get
     * the upcall.</td>
     * </tr>
     * <tr>
     * <td nowrap>-satin-queue-size <em>num</em></td>
     * <td>suggested job queue size.</tr>
     * <tr>
     * <td>-satin-alg <em>alg</em></td>
     * <td>there are currently three job-stealing algorithms: "RS" (random work
     * stealing), "CRS" (cluster aware random work stealing), and "MW" (master
     * worker).</td>
     * </tr>
     * <tr>
     * <td>-satin-kill <em>sec</em></td>
     * <td>crash after <em>sec</em> seconds; used for tests</td>
     * </tr>
     * <tr>
     * <td>-satin-delete <em>sec</em></td>
     * <td>leave after <em>sec</em> seconds; used for tests</td>
     * </tr>
     * <tr>
     * <td>-satin-grt-uses-stamps<em>sec</em></td>
     * <td>use the stamps instead of parameter records  as keys for jobs in
     * the global result table</td>
     * </tr>
     * <tr></table>
     */
    /*
     * Parse commandline parameters. Remove everything that starts with satin.
     */
    String[] parseArguments(String[] args, StaticProperties reqprops,
            Satin satin) {
        String alg = null;
        Vector tempArgs = new Vector();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-satin-closed")) {
                /* Closed world assumption. */
                closed = true;
            } else if (args[i].equals("-satin-panda")) {
                reqprops.add("name", "panda");
            } else if (args[i].equals("-satin-mpi")) {
                reqprops.add("name", "mpi");
            } else if (args[i].startsWith("-satin-net")) {
                reqprops.add("name", args[i].substring(7));
            } else if (args[i].equals("-satin-nio")) {
                reqprops.add("name", "nio");
            } else if (args[i].equals("-satin-tcp")) {
                reqprops.add("name", "tcp");
            } else if (args[i].equals("-satin-stats")) {
                stats = true;
            } else if (args[i].equals("-satin-no-stats")) {
                stats = false;
            } else if (args[i].equals("-satin-detailed-stats")) {
                stats = true;
                detailedStats = true;
            } else if (args[i].equals("-satin-ibis")) {
                if (sunSerialization) {
                    System.err.println("-satin-sun and -satin-ibis specified!");
                    System.exit(1); // Conflicting options
                }
                ibisSerialization = true;
            } else if (args[i].equals("-satin-sun")) {
                if (ibisSerialization) {
                    System.err.println("-satin-sun and -satin-ibis specified!");
                    System.exit(1); // Conflicting options
                }
                sunSerialization = true;
            } else if (args[i].equals("-satin-no-upcalls")) {
                upcalls = false;
            } else if (args[i].equals("-satin-upcalls")) {
                upcalls = true;
            } else if (args[i].equals("-satin-upcall-polling")) {
                upcallPolling = true;
            } else if (args[i].equals("-satin-queue-size")) {
                i++;
                try {
                    suggestedQueueSize = Integer.parseInt(args[i]);
                } catch (Exception e) {
                    System.err
                        .println("Option -satin-queue-size needs integer "
                            + "parameter.");
                    System.exit(1); // Wrong option
                }
            } else if (args[i].equals("-satin-alg")) {
                i++;
                alg = args[i];
            } else if (args[i].equals("-satin-grt-uses-stamps")) {
                grtUsesStamps = true;
            } else if (args[i].equals("-satin-delete")) {
                i++;
                try {
                    deleteTime = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("SATIN: invalid delete time");
                }
            } else if (args[i].equals("-satin-delete-cluster")) {
                i++;
                try {
                    deleteClusterTime = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("SATIN: invalid delete time");
                }
            } else if (args[i].equals("-satin-kill")) {
                i++;
                try {
                    killTime = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("SATIN: invalid kill time");
                }
            } else if (args[i].equals("-satin-kill-cluster")) {
                i++;
                try {
                    killTime = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("SATIN: invalid kill time");
                }
                i++;
                killCluster = args[i];
            } else if (args[i].equals("-satin-dump")) {
                dump = true;
            } else if (args[i].equals("-satin-initial-node")) {
                initialNode = true;
                getTable = false;
            } else if (args[i].equals("-satin-so-delay")) {
                i++;
                try {
                    soInvocationsDelay = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("SATIN: invalid so delay");
                }
            } else if (args[i].equals("-satin-so-size")) {
                i++;
                try {
                    soMaxMessageSize = Integer.parseInt(args[i]) * 1024;
                } catch (NumberFormatException e) {
                    System.err.println("SATIN: invalid so size");
                }
            } else {
                tempArgs.add(args[i]);
            }
        }

        if (alg == null) {
            if (master) {
                System.err.println("SATIN '" + "- "
                    + "': satin_algorithm property not specified, "
                    + "using RS");
            }
            alg = "RS";
        }

        if (alg.equals("RS")) {
            algorithm = new RandomWorkStealing(satin);
        } else if (alg.equals("CRS")) {
            algorithm = new ClusterAwareRandomWorkStealing(satin);
        } else if (alg.equals("MW")) {
            algorithm = new MasterWorker(satin);
        } else {
            System.err.println("SATIN '" + "- " + "': satin_algorithm '"
                + alg + "' unknown");
            algorithm = null;
            System.exit(1); // Wrong option
        }

        mergeUserPropertiesWithParams(reqprops);

        String[] unparsedOptions = new String[tempArgs.size()];
        for (int i = 0; i < tempArgs.size(); i++) {
            unparsedOptions[i] = (String) tempArgs.get(i);
        }

        return unparsedOptions;
    }

    void mergeUserPropertiesWithParams(StaticProperties reqprops) {
        // Combine old-style arguments with new style properties.
        StaticProperties userprops = StaticProperties.userProperties();
        String str = userprops.getProperty("worldmodel");
        if (str != null) {
            if (closed && !str.equals("closed")) {
                System.err.println("Inconsistent options: -satin-closed and "
                    + "-Dibis.worldmodel=" + str);
                System.exit(1); // Conflicting options
            }
            if (str.equals("closed")) {
                closed = true;
            }
        }
        str = userprops.getProperty("name");
        if (str != null) {
            String s2 = reqprops.getProperty("name");
            if (s2 != null && !s2.equals(str)) {
                System.err.println("Inconsistent options: -satin-" + s2
                    + " and -Dibis.name=" + str);
                System.exit(1); // Conflicting options
            }
        }
        str = userprops.getProperty("serialization");
        if (str != null) {
            if (ibisSerialization && !str.equals("ibis")) {
                System.err.println("Inconsistent options: -satin-ibis and "
                    + "-Dibis.serialization=" + str);
                System.exit(1); // Conflicting options
            }
            if (sunSerialization && !str.equals("sun")) {
                System.err.println("Inconsistent options: -satin-sun and "
                    + "-Dibis.serialization=" + str);
                System.exit(1); // Conflicting options
            }
            if (str.equals("ibis")) {
                ibisSerialization = true;
            }
            if (str.equals("sun")) {
                sunSerialization = true;
            }
        }
    }

    StaticProperties createIbisProperties(StaticProperties requestedProperties) {
        StaticProperties ibisProperties = new StaticProperties(
            requestedProperties);

        if (ibisSerialization) {
            ibisProperties.add("serialization", "byte, ibis");
        } else if (sunSerialization) {
            ibisProperties.add("serialization", "byte, sun");
        } else {
            ibisProperties.add("serialization", "byte, object");
        }

        if (closed) {
            ibisProperties.add("worldmodel", "closed");
        } else {
            ibisProperties.add("worldmodel", "open");
        }

        String commprops = "OneToOne, OneToMany, ManyToOne, ExplicitReceipt, Reliable";
        if (FAULT_TOLERANCE) {
            commprops += ", ConnectionUpcalls";
        }
        if (upcalls) {
            if (upcallPolling) {
                commprops += ", PollUpcalls";
            } else {
                commprops += ", AutoUpcalls";
            }
        }

        ibisProperties.add("communication", commprops);
        return ibisProperties;
    }

    PortType createSatinPortType(StaticProperties reqprops) throws IOException,
            IbisException {
        StaticProperties satinPortProperties = new StaticProperties(reqprops);

        if (closed) {
            satinPortProperties.add("worldmodel", "closed");
        } else {
            satinPortProperties.add("worldmodel", "open");
        }

        String commprops = "OneToOne, ManyToOne, ExplicitReceipt, Reliable";
        if (FAULT_TOLERANCE) {
            commprops += ", ConnectionUpcalls";
        }
        if (upcalls) {
            if (upcallPolling) {
                commprops += ", PollUpcalls";
            } else {
                commprops += ", AutoUpcalls";
            }
        }
        satinPortProperties.add("communication", commprops);

        if (ibisSerialization) {
            satinPortProperties.add("Serialization", "ibis");
            // if (master) {
            //     System.err.println("SATIN: using Ibis serialization");
            // }
        } else if (sunSerialization) {
            satinPortProperties.add("serialization", "sun");
        } else {
            satinPortProperties.add("serialization", "object");
        }

        return ibis.createPortType("satin porttype", satinPortProperties);
    }

    // The barrier port type is different from the satin port type.
    // It does not do multicast, and does not need serialization.
    PortType createBarrierPortType(StaticProperties reqprops)
            throws IOException, IbisException {
        StaticProperties s = new StaticProperties(reqprops);

        s.add("serialization", "byte");
        if (closed) {
            s.add("worldmodel", "closed");
        } else {
            s.add("worldmodel", "open");
        }

        s.add("communication", "OneToOne, ManyToOne, Reliable, "
            + "ExplicitReceipt");

        return ibis.createPortType("satin barrier porttype", s);
    }

    PortType createSOPortType(StaticProperties reqprops)
            throws IOException, IbisException {
        StaticProperties satinPortProperties = new StaticProperties(reqprops);

        if (closed) {
            satinPortProperties.add("worldmodel", "closed");
        } else {
            satinPortProperties.add("worldmodel", "open");
        }

        String commprops = "OneToOne, OneToMany, ExplicitReceipt, Reliable";
        if (FAULT_TOLERANCE) {
            commprops += ", ConnectionUpcalls";
        }
        if (upcalls) {
            if (upcallPolling) {
                commprops += ", PollUpcalls";
            } else {
                commprops += ", AutoUpcalls";
            }
        }
        satinPortProperties.add("communication", commprops);

        if (ibisSerialization) {
            satinPortProperties.add("Serialization", "ibis");
            // if (master) {
            //     System.err.println("SATIN: using Ibis serialization");
            // }
        } else if (sunSerialization) {
            satinPortProperties.add("Serialization", "sun");
        } else {
            satinPortProperties.add("serialization", "object");
        }

        return ibis.createPortType("satin SO porttype",
            satinPortProperties);
    }

    PortType createGlobalResultTablePortType(StaticProperties reqprops)
            throws IOException, IbisException {
        StaticProperties satinPortProperties = new StaticProperties(reqprops);

        if (closed) {
            satinPortProperties.add("worldmodel", "closed");
        } else {
            satinPortProperties.add("worldmodel", "open");
        }

        String commprops = "OneToOne, ManyToOne, ExplicitReceipt, Reliable";
        commprops += ", ConnectionUpcalls";
        if (upcalls) {
            if (upcallPolling) {
                commprops += ", PollUpcalls";
            } else {
                commprops += ", AutoUpcalls";
            }
        }
        satinPortProperties.add("communication", commprops);

        if (ibisSerialization) {
            satinPortProperties.add("Serialization", "ibis");
            // if (master) {
            //     System.err.println("SATIN: using Ibis serialization");
            // }
        } else if (sunSerialization) {
            satinPortProperties.add("Serialization", "sun");
        } else {
            satinPortProperties.add("serialization", "object");
        }

        return ibis.createPortType("satin global result table porttype",
            satinPortProperties);
    }
}
