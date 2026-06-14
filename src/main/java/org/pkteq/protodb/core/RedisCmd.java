package org.pkteq.protodb.core;

import java.util.List;

public record RedisCmd(String cmd, List<?> args) {

}
