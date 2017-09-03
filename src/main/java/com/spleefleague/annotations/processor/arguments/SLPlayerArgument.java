/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.annotations.processor.arguments;

import com.spleefleague.annotations.DispatchResult;
import com.spleefleague.annotations.DispatchResultType;
import com.spleefleague.annotations.processor.Processor;
import com.spleefleague.core.SpleefLeague;
import com.squareup.javapoet.MethodSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author jonas
 */
public class SLPlayerArgument extends CommandArgument {

    private final boolean exact;
    private final boolean offline;
    
    public SLPlayerArgument(Map<String, Object> values, TypeMirror type) {
        super(values, type);
        this.exact = super.getValue("exact");
        this.offline = super.getValue("offline");
        if(!exact && offline) {
            Processor.messager.printMessage(Diagnostic.Kind.WARNING, "Offline search only supports exact results.");
        }
    }

    @Override
    public void generateCode(MethodSpec.Builder builder, int paramId) {
        if(this.getType().getKind() == TypeKind.ARRAY) {
            generateArray(builder, paramId);
        }
        else {
            generateSingle(builder, paramId);
        }
    }
    
    
    
    private void generateArray(MethodSpec.Builder builder, int paramId) {
        builder.addStatement("$T<$T> parsed = new $T<>()", List.class, Player.class, ArrayList.class);    
        builder.beginControlFlow("while(position < args.length)");
            fetchOne(builder, "Player p", "args[position++]");
            builder.addStatement("if(p == null) break");
            builder.addStatement("parsed.add(p)");
        builder.endControlFlow();
        builder.addStatement("if(parsed.isEmpty()) return new $T(null, $T.$L)", 
                paramId,
                DispatchResult.class,
                DispatchResultType.class,
                DispatchResultType.NO_VALID_ROUTE
        );
        builder.addStatement("param$L = new $T[parsed.size()]", paramId, Player.class);
        builder.beginControlFlow("for(int i = 0; i < parsed.size(); i++)");
            builder.addStatement("param$L[i] = parsed.get(i)", paramId);
        builder.endControlFlow();
    }
    
    private void generateSingle(MethodSpec.Builder builder, int paramId) {
        consumeSafe(builder);
        fetchOne(builder, "param" + paramId, "arg");
        builder.addCode("if(param$L == null) ", paramId);
        builder.addStatement("return new $T($T.$L + args[position - 1] + $T.$L + $S, $T.$L)",
            DispatchResult.class,
            ChatColor.class,
            ChatColor.WHITE.name(),
            ChatColor.class,
            ChatColor.RED.name(),
            " is not online.",
            DispatchResultType.class,
            DispatchResultType.OTHER
        );
    }
    
    private void fetchOne(MethodSpec.Builder builder, String target, String name) {
        if(offline) {
            fetchOffline(builder, target, "arg");
        }
        else {
            fetchOnline(builder, target, "arg");
        }
    }
    
    private void fetchOnline(MethodSpec.Builder builder, String target, String name) {
        if(exact) {
            builder.addStatement("$L = $T.getInstance().getPlayerManager().get($L)", target, SpleefLeague.class, name);
        }
        else {
            builder.addStatement("$T p = $T.getPlayer($L)", Player.class, Bukkit.class, name);
            builder.beginControlFlow("if(p == null)");
                builder.addStatement("$L = null", target);
            builder.endControlFlow();
            builder.beginControlFlow("else");
                builder.addStatement("$L = $T.getInstance().getPlayerManager().get($L)", target, SpleefLeague.class, name);
            builder.endControlFlow();
        }
    }
    
    private void fetchOffline(MethodSpec.Builder builder, String target, String name) {
        fetchOnline(builder, target, name);
        builder.beginControlFlow("if($L == null)", target);
            builder.addStatement("$L = $T.getInstance().getPlayerManager().loadFake($L)", target, SpleefLeague.class, name);
        builder.endControlFlow();
    }
}
