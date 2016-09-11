/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2016 Neil C Smith.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 * 
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 * 
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import net.neilcsmith.praxis.code.CodeCompilerService;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.interfaces.Service;
import net.neilcsmith.praxis.hub.net.SlaveInfo;
import net.neilcsmith.praxis.logging.LogService;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class HubSlaveInfo extends SlaveInfo {

    private final String host;
    private final int port;
    private final String id;
    private final String type;
    private final boolean autoStart;

    private final Pattern idPattern;
    private final Pattern typePattern;

    public HubSlaveInfo(String host, int port, String id, String type, boolean autoStart) {
        super(new InetSocketAddress(host, port));

        validateHostString(host);

        this.host = host;
        this.port = port;
        this.id = id;
        this.type = type;
        this.autoStart = autoStart;

        idPattern = globToRegex(id);
        typePattern = globToRegex(type);
    }

    private void validateHostString(String host) {
        for (char c : host.toCharArray()) {
            if (Character.isWhitespace(c)) {
                throw new IllegalArgumentException("Host cannot contain whitespace");
            }
        }
    }

    private Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
//        boolean first = true;
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append('.');
                    break;
                case '|':
                    regex.append('|');
                    break;
                case '_':
                    regex.append('_');
                    break;
                case '-':
                    regex.append("\\-");
                    break;
                default:
                    if (Character.isJavaIdentifierPart(c)) {
                        regex.append(c);
                    } else {
                        throw new IllegalArgumentException();
                    }
            }
        }
        return Pattern.compile(regex.toString());
    }

    @Override
    public boolean matches(String rootID, ComponentType rootType) {
        if (!idPattern.matcher(rootID).matches()) {
            return false;
        }
        String tp = rootType.toString();
        if (!tp.startsWith("root:")) {
            return false;
        }
        tp = tp.substring(5);
        return typePattern.matcher(tp).matches();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    @Override
    public List<Class<? extends Service>> getRemoteServices() {
        if (HubSettings.getDefault().isUseMasterCompiler()) {
            List<Class<? extends Service>> list = new ArrayList<>(2);
            list.add(LogService.class);
            list.add(CodeCompilerService.class);
            return list;
        } else {
            return Collections.singletonList(LogService.class);
        }
    }

    @Override
    public boolean getUseLocalResources() {
        if (getUseRemoteResources()) {
            return HubSettings.getDefault().isPreferLocalFiles();
        } else {
            return true;
        }
    }

    @Override
    public boolean getUseRemoteResources() {
        return HubSettings.getDefault().isRunFileServer();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(host).append(' ')
                .append(port).append(' ')
                .append(id).append(' ')
                .append(type).append(' ')
                .append(autoStart);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.host);
        hash = 59 * hash + this.port;
        hash = 59 * hash + Objects.hashCode(this.id);
        hash = 59 * hash + Objects.hashCode(this.type);
        hash = 59 * hash + (this.autoStart ? 1 : 0);
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
        final HubSlaveInfo other = (HubSlaveInfo) obj;
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (this.autoStart != other.autoStart) {
            return false;
        }
        return true;
    }

    public static HubSlaveInfo fromString(String string) {
        String[] parts = string.split("\\s+");
        if (parts.length != 5) {
            throw new IllegalArgumentException();
        }
        int port = Integer.parseInt(parts[1]);
        boolean autoStart = Boolean.parseBoolean(parts[4]);
        return new HubSlaveInfo(parts[0], port, parts[2], parts[3], autoStart);
    }

}
