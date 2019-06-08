package com.mcf.davidee.nbteditpqb.nbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mcf.davidee.nbteditpqb.NBTEdit;
import com.mcf.davidee.nbteditpqb.NBTHelper;
import com.mcf.davidee.nbteditpqb.NBTStringHelper;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

public class NBTTree {
	
	private CompoundNBT baseTag;
	
	private Node<NamedNBT> root;
	
	public NBTTree (CompoundNBT tag){
		baseTag = tag;
		construct();
	}
	public Node<NamedNBT> getRoot(){
		return root;
	}
	
	public boolean canDelete(Node<NamedNBT> node){
		return node != root;
	}
	
	public boolean delete(Node<NamedNBT> node) {
		return !(node == null || node == root) && deleteNode(node, root);
	}
	
	private boolean deleteNode(Node<NamedNBT> toDelete, Node<NamedNBT> cur){
		for (Iterator<Node<NamedNBT>> it = cur.getChildren().iterator(); it.hasNext();){
			Node<NamedNBT> child = it.next();
			if (child == toDelete){
				it.remove();
				return true;
			}
			boolean flag = deleteNode(toDelete,child);
			if (flag)
				return true;
		}
		return false;
	}
	
	
	private void construct() {
		root = new Node<>(new NamedNBT("ROOT", baseTag.copy()));
		root.setDrawChildren(true);
		addChildrenToTree(root);
		sort(root);
	}
	
	public void sort(Node<NamedNBT> node) {
		Collections.sort(node.getChildren(), NBTEdit.SORTER);
		for (Node<NamedNBT> c : node.getChildren())
			sort(c);
	}
	
	public void addChildrenToTree(Node<NamedNBT> parent){
		INBT tag = parent.getObject().getNBT();
		if (tag instanceof CompoundNBT){
			Map<String,INBT> map =  NBTHelper.getMap((CompoundNBT)tag);
			for (Entry<String,INBT> entry : map.entrySet()){
				INBT base = entry.getValue();
				Node<NamedNBT> child = new Node<>(parent, new NamedNBT(entry.getKey(), base));
				parent.addChild(child);
				addChildrenToTree(child);
			}
			
		}
		else if (tag instanceof ListNBT){
			ListNBT list = (ListNBT)tag;
			for (int i =0; i < list.size(); ++ i){
				INBT base = NBTHelper.getTagAt(list, i);
				Node<NamedNBT> child = new Node<>(parent, new NamedNBT(base));
				parent.addChild(child);
				addChildrenToTree(child);
			}
		}
	}
	
	public CompoundNBT toCompoundNBT(){
		CompoundNBT tag = new CompoundNBT();
		addChildrenToTag(root, tag);
		return tag;
	}
	
	public void addChildrenToTag (Node<NamedNBT> parent, CompoundNBT tag){
		for (Node<NamedNBT> child : parent.getChildren()){
			INBT base = child.getObject().getNBT();
			String name = child.getObject().getName();
			if (base instanceof CompoundNBT){
				CompoundNBT newTag = new CompoundNBT();
				addChildrenToTag(child, newTag);
				tag.put(name, newTag);
			}
			else if (base instanceof ListNBT){
				ListNBT list = new ListNBT();
				addChildrenToList(child, list);
				tag.put(name, list);
			}
			else
				tag.put(name, base.copy());
		}
	}
	
	public void addChildrenToList(Node<NamedNBT> parent, ListNBT list){
		for (Node<NamedNBT> child: parent.getChildren()){
			INBT base = child.getObject().getNBT();
			if (base instanceof CompoundNBT){
				CompoundNBT newTag = new CompoundNBT();
				addChildrenToTag(child, newTag);
				list.appendTag(newTag);
			}
			else if (base instanceof ListNBT){
				ListNBT newList = new ListNBT();
				addChildrenToList(child, newList);
				list.appendTag(newList);
			}
			else
				list.appendTag(base.copy());
		}
	}
	
	public void print(){
		print(root,0);
	}
	
	private void print(Node<NamedNBT> n, int i){
		System.out.println(repeat("\t",i) + NBTStringHelper.getNBTName(n.getObject()));
		for (Node<NamedNBT> child : n.getChildren())
			print(child,i+1);
	}
	
	public List<String> toStrings(){
		List<String> s = new ArrayList<>();
		toStrings(s,root,0);
		return s;
	}
	
	private void toStrings(List<String> s, Node<NamedNBT> n, int i){
		s.add(repeat("   ",i) + NBTStringHelper.getNBTName(n.getObject()));
		for (Node<NamedNBT> child : n.getChildren())
			toStrings(s,child,i+1);
	}
	
	public static String repeat(String c, int i){
		StringBuilder b = new StringBuilder(i+1);
		for (int j =0; j < i; ++ j)
			b.append(c);
		return b.toString();
	}
}
