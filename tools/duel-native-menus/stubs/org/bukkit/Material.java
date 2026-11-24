package org.bukkit; public class Material { public final String name; private Material(String name){this.name=name;} public static Material matchMaterial(String name){return new Material(name);} }
